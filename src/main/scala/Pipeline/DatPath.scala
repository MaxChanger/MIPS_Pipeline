//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline DataPath Module
//
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util._

class DPathIo extends Bundle()
{
	val Inst 		= Input(UInt(32.W))		// The instructions to be taken are put here
	val boot		= Input(Bool())			//
	val reset		= Input(Bool())

	val imem_addr	= Output(UInt(32.W))	// Instruction memory address.
	val dmem_addr	= Output(UInt(32.W))	// Data memory address.
	val dmem_datIn	= Output(UInt(32.W))
	val dmem_datOut	= Input(UInt(32.W))
	
	val MemWrite    = Output(Bool())
	val MemRead     = Output(Bool())
	
	val Store_type  = Output(UInt(2.W))
	val Load_type 	= Output(UInt(3.W))

	val MEMWB_Inst     = Output(UInt(32.W))
	val BranchPrediction_Branch_success	= Output(UInt(32.W))
	val BranchPrediction_Branch_number	= Output(UInt(32.W))

}

class DatPath extends Module {

	import Control._
    import Instructions._
	import ALU._

	val io 				= IO( new DPathIo )

	val regfile 		= Module( new RegFile )
	val hazardcheck		= Module( new HazardCheck )
	val alu 			= Module( new ALU )
	val forward 		= Module( new Forward )
	val ctl     		= Module( new Control ) 
	val branch     		= Module( new Branch ) 
    val exceptioncheck 	= Module( new ExceptionCheck )
	val branchprediction= Module( new BranchPrediction )
    // printf("PredictAccuarcy = %d\n",  io.BranchPrediction_Branch_success)


	//**********************PC**********************//
	val pc_next 		= Wire(UInt(32.W))
	val pc_plus_4		= Wire(UInt(32.W))
	val pc_reg  		= RegInit(0.U(32.W))

	val IFID_pc_plus_4	= RegNext(next = pc_plus_4, init = 0.U(32.W))
	val IDEX_pc_plus_4	= RegNext(next = IFID_pc_plus_4, init = 0.U(32.W))
	val EXMEM_pc_plus_4	= RegNext(next = IDEX_pc_plus_4, init = 0.U(32.W))
	val MEMWB_pc_plus_4	= RegNext(next = EXMEM_pc_plus_4, init = 0.U(32.W))

	val IDEX_pc		= RegNext(next = pc_reg, init = 0.U(32.W))
	val EXMEM_pc	= RegNext(next = IDEX_pc, init = 0.U(32.W))
	val MEMWB_pc	= RegNext(next = EXMEM_pc, init = 0.U(32.W))



  	//********************** ID **********************//

  	val IFID_Rs 	= Wire(UInt(5.W))		// Three ports of RegFile
	val IFID_Rt 	= Wire(UInt(5.W))
	val IFID_Rd 	= Wire(UInt(5.W))	   
	val IFID_Imm16 	= Wire(UInt(16.W))		// 16 bit immediate number
	val IFID_Imm26 	= Wire(UInt(26.W))		// 26 bit immediate number 
	val IF_reg 		= Wire(UInt(32.W))
	val IFID_Inst 	= RegNext(next = IF_reg, init = 0.U(32.W))

	// Control_Remove used for stall the pipeline
	val flag 		= (hazardcheck.io.Control_Remove || branch.io.ID_flush || exceptioncheck.io.ID_flush)	
	
	// Register Of Control Part
	val IFID_NOP			= Mux(flag, false.B, ctl.io.NOP)
	val	IFID_Jump			= Mux(flag, 0.U(2.W), ctl.io.Jump)
	val	IFID_ReadData1_Src 	= Mux(flag, 0.U(2.W), ctl.io.ReadData1_Src)
	val	IFID_ExtOp			= Mux(flag, false.B, ctl.io.ExtOp)
	
	val	IFID_ALUSrc			= Mux(flag, 0.U(2.W), ctl.io.ALUSrc)
	val	IFID_RegDst			= Mux(flag, false.B , ctl.io.RegDst)
	val	IFID_ALUOp			= Mux(flag, 0.U(5.W), ctl.io.ALUOp)

	val	IFID_MemWrite		= Mux(flag, false.B, ctl.io.MemWrite)
	val	IFID_MemRead		= Mux(flag, false.B, ctl.io.MemRead)
	val	IFID_Store_type		= Mux(flag, 0.U(2.W), ctl.io.Store_type)
	val	IFID_Load_type		= Mux(flag, 0.U(3.W), ctl.io.Load_type)
	val	IFID_Branch_type	= Mux(flag, 0.U(4.W), ctl.io.Branch_type)

	val	IFID_MemtoReg		= Mux(flag, 0.U(2.W), ctl.io.MemtoReg)
	val	IFID_HI_Wr			= Mux(flag, false.B, ctl.io.HI_Wr)
	val	IFID_LO_Wr			= Mux(flag, false.B, ctl.io.LO_Wr)
	val	IFID_If31			= Mux(flag, false.B, ctl.io.If31)
	val	IFID_RegWrite		= Mux(flag, false.B, ctl.io.RegWrite)

	val	IFID_CP0_Read		= Mux(flag, false.B, ctl.io.CP0_Read)
	val	IFID_CP0_Write		= Mux(flag, false.B, ctl.io.CP0_Write)

	val IFID_ReadData1		= Wire(UInt(32.W))
	val IFID_ReadData2		= Wire(UInt(32.W))

	val IFID_HI_Read		= (IFID_ReadData1_Src === Read_HI )
	val IFID_LO_Read		= (IFID_ReadData1_Src === Read_LO )

	
	//********************** EX **********************//

	//ALU Module
	val SrcA 	= Wire(UInt(32.W))
	val SrcB 	= Wire(UInt(32.W))
	val ALUctr  = Wire(UInt(5.W))
	
	val HI_reg	= RegInit(0.U(32.W))
	val LO_reg	= RegInit(0.U(32.W))
	
	val IDEX_Inst 		= RegNext(next = IFID_Inst, init = 0.U(32.W))

	when(exceptioncheck.io.ID_flush === true.B){
		IFID_Inst := 0.U
		IDEX_Inst := 0.U
	}

	val IDEX_Rs 		= RegNext(next = IFID_Rs, init = 0.U(5.W))
	val IDEX_Rt 		= RegNext(next = IFID_Rt, init = 0.U(5.W))
	val IDEX_Rd 		= RegNext(next = IFID_Rd, init = 0.U(5.W))
	val IDEX_Imm26		= RegNext(next = IFID_Imm26, init = 0.U(26.W))
	val IDEX_If31		= RegNext(next = IFID_If31, init = false.B)

	val	IDEX_CP0_Read		= RegNext(next = IFID_CP0_Read, init = false.B)
	val	IDEX_CP0_Write		= RegNext(next = IFID_CP0_Write, init = false.B)

	val	IDEX_ReadData1_Src 	= RegNext(next = IFID_ReadData1_Src, init = 0.U(2.W))


	val Imm16_To_IDEX 	= Wire(UInt(32.W)) // Mux(IFID_ExtOp, SignImm16 , UnSignImm16)
	val IDEX_Imm32		= RegNext(next = Imm16_To_IDEX, init = 0.U(32.W))
	
	val temp_SrcB = Wire(UInt(32.W))

	val IDEX_ReadData1	= RegNext(next = IFID_ReadData1, init = 0.U(32.W))
	val IDEX_ReadData2	= RegNext(next = IFID_ReadData2, init = 0.U(32.W))
	
	val IDEX_ALUOut 	= Wire(UInt(64.W))
	val IDEX_zero		= Wire(Bool())
	val IDEX_ADDOut		= Wire(UInt(32.W))
	
	val IDEX_HI_Read		= RegNext(next = IFID_HI_Read, init = false.B)
	val IDEX_LO_Read		= RegNext(next = IFID_LO_Read, init = false.B)
	


	// 1
	val IDEX_Jump		= RegNext(next = IFID_Jump, init = false.B)

	val IDEX_RegDst		= RegNext(next = IFID_RegDst, init = false.B)
	val IDEX_ALUOp		= RegNext(next = IFID_ALUOp,  init = "b00000".U(5.W))
	val IDEX_ALUSrc		= RegNext(next = IFID_ALUSrc, init = "b00".U(2.W))

	val IDEX_MemWrite	= RegNext(next = IFID_MemWrite, init = false.B)
	val IDEX_MemRead	= RegNext(next = IFID_MemRead,  init = false.B)	

	val IDEX_RegWrite	= RegNext(next = IFID_RegWrite, init = false.B)
	val IDEX_MemtoReg	= RegNext(next = IFID_MemtoReg, init = 0.U(2.W))
	
	val IDEX_Store_type	= RegNext(next = IFID_Store_type, init = "b00".U(2.W))
	val IDEX_Load_type	= RegNext(next = IFID_Load_type,  init = "b000".U(3.W))
	val IDEX_Branch_type= RegNext(next = IFID_Branch_type,init = "b0000".U(4.W))


	val IDEX_reg_indx 	= Mux(IDEX_RegDst, IDEX_Rt, IDEX_Rd)	

	val IDEX_HI_Wr		= RegNext(next = IFID_HI_Wr, init = false.B)
	val IDEX_LO_Wr		= RegNext(next = IFID_LO_Wr, init = false.B)

	//********************** MEM **********************//

	val EXMEM_Rs 	  	= RegNext(next = IDEX_Rs, init = 0.U(5.W))
	val EXMEM_Rt 	  	= RegNext(next = IDEX_Rt, init = 0.U(5.W))
	val EXMEM_Rd 	  	= RegNext(next = IDEX_Rd, init = 0.U(5.W))
	val EXMEM_Imm32		= RegNext(next = IDEX_Imm32, init = 0.U(32.W))
	val EXMEM_If31		= RegNext(next = IDEX_If31,  init = false.B)

	val	EXMEM_CP0_Read	= RegNext(next = IDEX_CP0_Read, init = false.B)
	val	EXMEM_CP0_Write	= RegNext(next = IDEX_CP0_Write, init = false.B)

	val EXMEM_ReadData2 = RegNext(next = temp_SrcB,   init = 0.U(32.W)) // temp_SrcB  IDEX_ReadData2
	val	EXMEM_ALUOut 	= RegNext(next = IDEX_ALUOut, init = 0.U(64.W))
	val EXMEM_zero		= RegNext(next = IDEX_zero,   init = false.B)

	val EXMEM_reg_indx 	= RegNext(next = IDEX_reg_indx, init = 0.U(5.W))

	// 2
	val EXMEM_Jump		= RegNext(next = IDEX_Jump, init = false.B)
	val EXMEM_ALUOp		= RegNext(next = IDEX_ALUOp,  init = "b00000".U(5.W))

	// val EXMEM_MemWrite	= RegNext(next = IDEX_MemWrite, init = false.B)
	// val EXMEM_MemRead	= RegNext(next = IDEX_MemRead, init = false.B)	
	// val EXMEM_RegWrite	= RegNext(next = IDEX_RegWrite, init = false.B)
	// val EXMEM_MemtoReg	= RegNext(next = IDEX_MemtoReg, init = false.B)

	// val EXMEM_MemWrite	= Mux(branch.io.EX_flush , 0.U , RegNext(next = IDEX_MemWrite, init = false.B) )
	// val EXMEM_MemRead	= Mux(branch.io.EX_flush , 0.U , RegNext(next = IDEX_MemRead, init = false.B) )
	// val EXMEM_RegWrite	= Mux(branch.io.EX_flush , 0.U , RegNext(next = IDEX_RegWrite, init = false.B) )
	// val EXMEM_MemtoReg	= Mux(branch.io.EX_flush , 0.U , RegNext(next = IDEX_MemtoReg, init = 0.U(2.W)) )

	val temp_IDEX_MemWrite	= Mux(exceptioncheck.io.EX_flush , 0.U , IDEX_MemWrite )
	val temp_IDEX_MemRead	= Mux(exceptioncheck.io.EX_flush , 0.U , IDEX_MemRead )
	val temp_IDEX_RegWrite	= Mux(exceptioncheck.io.EX_flush , 0.U , IDEX_RegWrite )
	val temp_IDEX_MemtoReg	= Mux(exceptioncheck.io.EX_flush , 0.U , IDEX_MemtoReg )

	val EXMEM_MemWrite	= RegNext(next = temp_IDEX_MemWrite, init = false.B)
	val EXMEM_MemRead	= RegNext(next = temp_IDEX_MemRead, init = false.B)	
	val EXMEM_RegWrite	= RegNext(next = temp_IDEX_RegWrite, init = false.B)
	val EXMEM_MemtoReg	= RegNext(next = temp_IDEX_MemtoReg, init = false.B)


	val EXMEM_Store_type	= RegNext(next = IDEX_Store_type, init = "b00".U(2.W))
	val EXMEM_Load_type		= RegNext(next = IDEX_Load_type,  init = "b000".U(3.W))
	val EXMEM_Branch_type	= RegNext(next = IDEX_Branch_type,init = "b0000".U(4.W))

	val EXMEM_ADDOut  	= RegNext(next = IDEX_ADDOut, init = 0.U(32.W))

	val EXMEM_HI_Wr		= RegNext(next = IDEX_HI_Wr, init = false.B)
	val EXMEM_LO_Wr		= RegNext(next = IDEX_LO_Wr, init = false.B)

	val EXMEM_Inst 		= RegNext(next = IDEX_Inst, init = 0.U(32.W))


	//********************** WB **********************//

	val MEMWB_Rs 	  	= RegNext(next = EXMEM_Rs, init = 0.U(5.W))
	val MEMWB_Rt 	  	= RegNext(next = EXMEM_Rt, init = 0.U(5.W))
	val MEMWB_Rd 	  	= RegNext(next = EXMEM_Rd, init = 0.U(5.W))
	val MEMWB_If31		= RegNext(next = EXMEM_If31, init = false.B)

	val	MEMWB_CP0_Read		= RegNext(next = EXMEM_CP0_Read, init = false.B)
	val	MEMWB_CP0_Write		= RegNext(next = EXMEM_CP0_Write, init = false.B)


	val MEMWB_ALUOut 	= RegNext(next = EXMEM_ALUOut, init = 0.U(64.W))
	val MEMWB_reg_indx  = RegNext(next = EXMEM_reg_indx, init = 0.U(5.W))

	// 3	
	val MEMWB_ALUOp		= RegNext(next = EXMEM_ALUOp,  init = "b00000".U(5.W))



	val MEMWB_RegWrite		= Mux(branch.io.EX_flush , 0.U , RegNext(next = EXMEM_RegWrite, init = false.B) )
	val MEMWB_MemtoReg		= Mux(branch.io.EX_flush , 0.U , RegNext(next = EXMEM_MemtoReg, init = 0.U(2.W)) )
	
	val MEMWB_Store_type	= RegNext(next = EXMEM_Store_type, init = "b00".U(2.W))
	val MEMWB_Load_type		= RegNext(next = EXMEM_Load_type, 	init = "b000".U(3.W))
	// val MEMWB_Branch_type	= RegNext(next = EXMEM_Branch_type,init = "b0000".U(4.W))

	val MEMWB_HI_Wr		= RegNext(next = EXMEM_HI_Wr, init = false.B)
	val MEMWB_LO_Wr		= RegNext(next = EXMEM_LO_Wr, init = false.B)

	val MEMWB_ReadData 	= RegNext(next = io.dmem_datOut, init = 0.U(32.W))

	val Reg_WriteData 	= Wire(UInt(64.W))


	val MEMWB_Inst 		= RegNext(next = EXMEM_Inst, init = 0.U(32.W))
	
	io.MEMWB_Inst		:=	MEMWB_Inst	// Write to Pipeline Top Test for Pipelinetoptest to Check if Through Testing
	


	
	//********************** PC **********************//

	pc_plus_4	:= pc_reg + 4.U	// PC + 4

	// When writing instruction data to CPU but not running/computing
	when (io.boot === true.B) {
		pc_next := 0.U
		pc_reg 	:= 0.U
	}

	val EXMEM_offset_00 =  Cat(EXMEM_Imm32(29,0),"b00".U(2.W)) + EXMEM_pc_plus_4 

	when(io.boot === false.B){
	// printf("Inst:%x\n",io.Inst)

		// If want to stall the pipeline , pc_reg = pc_reg , It's not equal to 0.
		pc_reg := MuxCase( Mux(hazardcheck.io.PCWr, pc_next, pc_reg), 
									 Array((exceptioncheck.io.ExcepSrc === 0.U) ->  Mux(hazardcheck.io.PCWr, pc_next, pc_reg),
								 		   (exceptioncheck.io.ExcepSrc === 1.U) ->  16560.U(32.W),
								 		   (exceptioncheck.io.ExcepSrc === 2.U) ->  (exceptioncheck.io.EPCOut + 4.U) ))

		val Jump_addr	= Cat(IFID_Inst(31,28),IFID_Imm26(25,0),Fill(2,0.U(1.W)))

		val tempflag	= (IFID_Jump === J_RS && IFID_Rs === IDEX_reg_indx && IDEX_MemtoReg === WB_ALU && IDEX_RegWrite === true.B)
		
		pc_next := MuxCase(pc_plus_4, Array((IFID_Jump === J_XXX) ->  Mux(branch.io.PCSrc, EXMEM_offset_00, pc_plus_4),
								 			(IFID_Jump === J_IMM) ->  Jump_addr,
								 			(IFID_Jump === J_RS ) ->  Mux(tempflag, IDEX_ALUOut ,regfile.io.rdata1) ))

	}
	
	// Instruction Memory 
	io.imem_addr	:= pc_reg
			
	when(hazardcheck.io.IFID_Write === true.B){
		IF_reg := Mux((IFID_NOP || branch.io.IF_flush || exceptioncheck.io.IF_flush), 0.U(32.W), io.Inst )
	}.otherwise{
		IF_reg := IFID_Inst		// if stall the pipeline , IFID_Inst should keep one cycle Instand of equal 0
	}

	
	
	//********************** IF/ID **********************//
	
	ctl.io.Inst	:= IFID_Inst

  	IFID_Rs 	:= IFID_Inst(25, 21)	// Three ports of RegFile
	IFID_Rt 	:= IFID_Inst(20, 16)
	IFID_Rd 	:= IFID_Inst(15, 11)	   
	IFID_Imm16 	:= IFID_Inst(15, 0)		// 16 bit immediate number
	IFID_Imm26 	:= IFID_Inst(25, 0)		// 26 bit immediate number

	
	//Register File Module 
	regfile.io.raddr1 		:= IFID_Rs
	regfile.io.raddr2 		:= IFID_Rt
	regfile.io.waddr  		:= Mux(MEMWB_If31, 31.U(32.W), MEMWB_reg_indx)
	regfile.io.wdata  		:= Mux(MEMWB_If31, MEMWB_pc_plus_4 + 4.U, Reg_WriteData(31,0))
								// Notice the selector here. Bgezal writes back the data of register 31. is pc + 8
	regfile.io.RegWrite 	:= MEMWB_RegWrite

	when(MEMWB_HI_Wr === true.B){
		HI_reg := Reg_WriteData(63,32)
		// HI_reg := MuxCase(Reg_WriteData(63,32), Array((MEMWB_ALUOp === ALU_DIV) ->  Reg_WriteData(31,0),
		// 											  (MEMWB_ALUOp === ALU_DIVU) ->  Reg_WriteData(31,0),
		// 											  (MEMWB_ALUOp === ALU_MULT) ->  Reg_WriteData(63,32),
		// 						 					  (MEMWB_ALUOp === ALU_MULTU ) -> Reg_WriteData(63,32)  ))
	}
	when(MEMWB_LO_Wr === true.B){
		LO_reg := Reg_WriteData(31,0)
		// LO_reg := MuxCase(Reg_WriteData(31,0), Array((MEMWB_ALUOp === ALU_DIV) ->  Reg_WriteData(63,32),
		// 											  (MEMWB_ALUOp === ALU_DIVU) ->  Reg_WriteData(63,32),
		// 											  (MEMWB_ALUOp === ALU_MULT) ->  Reg_WriteData(31,0),
		// 						 					  (MEMWB_ALUOp === ALU_MULTU ) -> Reg_WriteData(31,0)  ))
	}

	// Sign extension 
	val SignImm16 		= Cat(Fill(16, IFID_Imm16(15)), IFID_Imm16(15, 0)) 	// Sign bit extension 
	val UnSignImm16		= Cat(Fill(16, 0.U(1.W)), IFID_Imm16(15, 0)) 		// Unsigned bit extension
	
	// Select by IFID_ExtOp controller signal
	Imm16_To_IDEX 		:= Mux(IFID_ExtOp, UnSignImm16 ,  SignImm16)


	// Hazardcheck module
	hazardcheck.io.IDEX_MemRead := IDEX_MemRead
	hazardcheck.io.IDEX_Rt    	:= IDEX_Rt
	hazardcheck.io.IFID_Rs 	  	:= IFID_Rs
	hazardcheck.io.IFID_Rt    	:= IFID_Rt


	////  CP0
	// exceptioncheck.io.in1			:= Mux((exceptioncheck.io.ExcepSrc =/= 0.U),0.U,SrcA)
	// exceptioncheck.io.in2			:= Mux((exceptioncheck.io.ExcepSrc =/= 0.U),0.U,SrcB)
	// exceptioncheck.io.Instruction 	:= Mux((exceptioncheck.io.ExcepSrc =/= 0.U),0.U,IDEX_Inst)
	// // exceptioncheck.io.ALUOut 		:= Mux((exceptioncheck.io.ExcepSrc =/= 0.U),0.U,alu.io.ALUOut)	// Cycle error
	// exceptioncheck.io.PC 			:= Mux((exceptioncheck.io.ExcepSrc =/= 0.U),0.U,IDEX_pc_plus_4)


	// Exceptioncheck Module
	exceptioncheck.io.Rd 	 	:= Mux((exceptioncheck.io.ExcepSrc === 1.U),0.U,IFID_Rd)
	exceptioncheck.io.CP0_Read 	:= Mux((exceptioncheck.io.ExcepSrc === 1.U),0.U,IFID_CP0_Read)
	exceptioncheck.io.CP0_Write	:= Mux((exceptioncheck.io.ExcepSrc === 1.U),0.U,MEMWB_CP0_Write)
	exceptioncheck.io.Addr	 	:= Mux((exceptioncheck.io.ExcepSrc === 1.U),0.U,MEMWB_reg_indx)
	exceptioncheck.io.DataIn 	:= Mux((exceptioncheck.io.ExcepSrc === 1.U),0.U,Reg_WriteData) 

	exceptioncheck.io.in1			:= SrcA
	exceptioncheck.io.in2			:= SrcB
	exceptioncheck.io.Instruction 	:= IDEX_Inst
	exceptioncheck.io.Previous_Inst	:= EXMEM_Inst
	
	exceptioncheck.io.ALUOut 		:= alu.io.ALUOut
	exceptioncheck.io.PC 			:= (IDEX_pc_plus_4 - 4.U) 
	// The PC to expectioncheck is the PC Current instruction. is PC not pc_plus_4, so I sub 4.U in here

	// For Sw Forwarding
	// when(EXMEM_RegWrite && (IFID_Rt === EXMEM_reg_indx)){
	// 	IDEX_ReadData2 	:=	EXMEM_ALUOut
	// }


	// Used for RegFile When Read and Write at same time
	// When  Read_Enalbe  and the read address 1 and the write address is the same	
	// MEMWB_RegWrite === true.B  			Notice	！！！This condition is necessary！！！
	val temp_ReadData1 = Mux(( MEMWB_RegWrite === true.B && (regfile.io.raddr1 === regfile.io.waddr)), 
																Reg_WriteData(31,0), regfile.io.rdata1)
	
	IFID_ReadData1	:= MuxCase(temp_ReadData1, Array((IFID_ReadData1_Src === Read_REG) -> temp_ReadData1,
													 (IFID_ReadData1_Src === Read_HI ) -> HI_reg,
													 (IFID_ReadData1_Src === Read_LO ) -> LO_reg,
													 (IFID_ReadData1_Src === Read_CP0) -> exceptioncheck.io.DataOut))

	// When  Read_Enalbe  and the read address 2 and the write address is the same	
	IFID_ReadData2  := Mux(( MEMWB_RegWrite === true.B && regfile.io.raddr2 === regfile.io.waddr), 
																	Reg_WriteData(31,0), regfile.io.rdata2)

	
	
	//**********************ID/EX**********************//
	//ADD Module
	IDEX_ADDOut	:=  Cat(IDEX_Imm32(29,0),"b00".U(2.W)) + IDEX_pc_plus_4		// PC + Imm << 2

	// Forward Signal
	forward.io.IDEX_Rs 			:= IDEX_Rs
	forward.io.IDEX_Rt 			:= IDEX_Rt	
	forward.io.EXMEM_reg_indx 	:= EXMEM_reg_indx
	forward.io.EXMEM_RegWrite 	:= EXMEM_RegWrite
	forward.io.MEMWB_reg_indx  	:= MEMWB_reg_indx
	forward.io.MEMWB_RegWrite 	:= MEMWB_RegWrite

	// Used for HI/LO
	forward.io.EXMEM_HI_Wr 	:= EXMEM_HI_Wr
	forward.io.EXMEM_LO_Wr 	:= EXMEM_LO_Wr
	forward.io.MEMWB_HI_Wr 	:= EXMEM_HI_Wr
	forward.io.MEMWB_LO_Wr 	:= MEMWB_LO_Wr
	forward.io.IDEX_HI_Read := IDEX_HI_Read
	forward.io.IDEX_LO_Read := IDEX_LO_Read

	// Used for CP0
	forward.io.EXMEM_CP0_Wr 	:= EXMEM_CP0_Write
	forward.io.MEMWB_CP0_Wr 	:= MEMWB_CP0_Write
	forward.io.IDEX_CP0_Read	:= IDEX_CP0_Read



	//ALU Forwarding ---- Mux(sel, inTrue, inFalse)
	SrcA :=	MuxCase(IDEX_ReadData1, Array(
					(forward.io.ForwardA === 0.U(2.W)) -> IDEX_ReadData1,
					(forward.io.ForwardA === 1.U(2.W)) -> Reg_WriteData(31,0),
					(forward.io.ForwardA === 2.U(2.W)) -> EXMEM_ALUOut(31,0)))

	temp_SrcB := MuxCase(IDEX_ReadData2, Array(
									(forward.io.ForwardB === 0.U(2.W)) -> IDEX_ReadData2,
									(forward.io.ForwardB === 1.U(2.W)) -> Reg_WriteData(31,0),
									(forward.io.ForwardB === 2.U(2.W)) -> EXMEM_ALUOut(31,0)))
	
	SrcB := MuxCase(temp_SrcB, Array((IDEX_ALUSrc === ALUSrc_FW )  -> temp_SrcB,
								     (IDEX_ALUSrc === ALUSrc_IMM)  -> IDEX_Imm32,
									 (IDEX_ALUSrc === ALUSrc_ZERO) -> 0.U(32.W)))


	alu.io.ALUctr	:= IDEX_ALUOp
	alu.io.in1		:= SrcA
	alu.io.in2		:= SrcB
	alu.io.shamt 	:= IDEX_Imm32(10,6)
	IDEX_zero 		:= alu.io.cmp_out	// Comparison results of output
	IDEX_ALUOut 	:= alu.io.ALUOut	// ALU Out
	
	branchprediction.io.reset			:= io.reset
	branchprediction.io.PC				:= (IDEX_pc_plus_4 - 4.U)	// Current Pc not pc_plus_4
	branchprediction.io.Instruction		:= IDEX_Inst
	branchprediction.io.Branch_type		:= IDEX_Branch_type
	branchprediction.io.ALUOut			:= IDEX_ADDOut
	branchprediction.io.zero			:= IDEX_zero
	io.BranchPrediction_Branch_success	:= branchprediction.io.Branch_success
	io.BranchPrediction_Branch_number	:= branchprediction.io.Branch_number


	//**********************MEM/WB**********************//
	
	// Data Memory
	io.dmem_datIn 	:= EXMEM_ReadData2 ///
	io.dmem_addr 	:= EXMEM_ALUOut(31,0)
	io.MemWrite     := EXMEM_MemWrite
	io.MemRead      := EXMEM_MemRead
	io.Store_type   := EXMEM_Store_type
	io.Load_type    := EXMEM_Load_type


	// Branch module
	branch.io.ALUOut 		:= EXMEM_ALUOut
	branch.io.zero  	 	:= EXMEM_zero
	branch.io.Branch_type 	:= EXMEM_Branch_type


	Reg_WriteData := MuxCase(MEMWB_ALUOut, Array((MEMWB_MemtoReg === WB_ALU) -> MEMWB_ALUOut,
												 (MEMWB_MemtoReg === WB_MEM) -> Cat(0.U(32.W),MEMWB_ReadData),
												 (MEMWB_MemtoReg === WB_PC4) -> Cat(0.U(32.W),MEMWB_pc_plus_4)))

}