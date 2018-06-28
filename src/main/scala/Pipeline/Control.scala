//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline Control Module
//
// Jiadai Sun  
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util.ListLookup
import ALU._
import Instructions._

object Control {

	// Yes or No
  	val T 	= true.B
  	val F 	= false.B
	
	// Jump
	val J_XXX  = 0.U(2.W)
	val J_IMM  = 1.U(2.W)
	val J_RS   = 2.U(2.W)
	
	// ReadData1_Src 
	val Read_XXX   	= 0.U(2.W)
	val Read_REG  	= 0.U(2.W)
	val Read_HI 	= 1.U(2.W)
	val Read_LO 	= 2.U(2.W)
	val Read_CP0    = 3.U(2.W)

	// ExtOp	Whether or not a symbolic extension is carried out
	val EXT_XXX 	= false.B
	val EXT_Sign 	= false.B
	val EXT_UnSi	= true.B

  	// ALUSrc
  	val ALUSrc_XXX  = 0.U(2.W)
  	val ALUSrc_FW  	= 0.U(2.W)
  	val ALUSrc_IMM  = 1.U(2.W)
  	val ALUSrc_ZERO = 2.U(2.W)

  	// RegDst	Select reg_indx is Rt or Rd
  	val DST_XX = false.B
  	val DST_Rd = false.B
  	val DST_Rt = true.B


  	// Store_type
  	val St_XX  = 0.U(2.W)
  	val St_SW  = 1.U(2.W)
  	val St_SH  = 2.U(2.W)
  	val St_SB  = 3.U(2.W)


  	// Load_type
  	val Ld_XXX = 0.U(3.W)
  	val Ld_LW  = 1.U(3.W)
  	val Ld_LH  = 2.U(3.W)
  	val Ld_LB  = 3.U(3.W)
  	val Ld_LHU = 4.U(3.W)
  	val Ld_LBU = 5.U(3.W)

  	// Branch_type
 	val BR_XXXX		= 0.U(4.W)
  	val BR_BEQ  	= 1.U(4.W)	// if GPR[Rs] === GPR[Rt]
  	val BR_BNE  	= 2.U(4.W)	// if GPR[Rs] =/= GPR[Rt]
  	val BR_BGEZ		= 3.U(4.W)  // if GPR[Rs] >= 0
  	val BR_BGTZ 	= 4.U(4.W)  // if GPR[Rs] >  0
  	val BR_BLEZ 	= 5.U(4.W)  // if GPR[Rs] <= 0
  	val BR_BLTZ 	= 6.U(4.W)  // if GPR[Rs] <  0
  	val BR_BGEZAL 	= 7.U(4.W)  // if GPR[Rs] >= 0 & PC+4 -> Regfile[31]
  	val BR_BLTZAL 	= 8.U(4.W)  // if GPR[Rs] <= 0 & PC+4 -> Regfile[31]


	// MemtoReg	WB Select the write back data source
	val WB_XXX = 0.U(2.W)
    val WB_ALU = 0.U(2.W)
 	val WB_MEM = 1.U(2.W)
  	val WB_PC4 = 2.U(2.W)


  	//RegWrite
  	val REG_XX = false.B
  	val REG_32 = false.B
  	val REG_HL = true.B



  	val default =														
	// 																											HI_Wr	   CP0_Read CP0_
    //                      ReadData1 	                      	          MemWrite  Store_type  Branch_type    	   | LO RegWrite  | Write
    //           NOP  Jump    _Src    ExtOp    ALUSrc     RegDst   ALUOp    | MemRead | Load_type    |   MemToReg  | _Wr |	If31  |  |
    //            |    |      |         |         |          |       |      |   |     |     |        |       |     |  |  |   |    |  |
             List(F, J_XXX, Read_XXX, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_XXX, F,  F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F,  F,   F, F)
 	val map = Array(

	// NOP
	NOP   -> List(F, J_XXX, Read_XXX, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_XXX, F, F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F, F,F,F,F, F),
	// Arithmetical instruction
    ADD   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_ADD, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
    ADDI  -> List(F, J_XXX, Read_REG, EXT_Sign,ALUSrc_IMM, DST_Rt, ALU_ADD, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
    ADDU  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_ADD, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
    ADDIU -> List(F, J_XXX, Read_REG, EXT_Sign,ALUSrc_IMM, DST_Rt, ALU_ADD, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
   
    SUB   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_SUB, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
    SUBU  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_SUB, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
    
	SLT   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_SLT, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
	SLTI  -> List(F, J_XXX, Read_REG, EXT_Sign,ALUSrc_IMM, DST_Rt, ALU_SLT, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
    SLTU  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_SLTU, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
    SLTIU -> List(F, J_XXX, Read_REG, EXT_Sign,ALUSrc_IMM, DST_Rt, ALU_SLTU, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F,F,F),
   
   
    DIV   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_XX, ALU_DIV, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, T, T, F, F,F,F,F,F),
    DIVU  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_XX, ALU_DIVU, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, T, T, F, F,F,F,F,F),
    MULT  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_XX, ALU_MULT, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, T, T, F, F,F,F,F,F),
    MULTU -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_XX, ALU_MULTU, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, T, T, F, F,F,F,F,F),
   
    // Logical instruction
	AND   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_AND, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    ANDI  -> List(F, J_XXX, Read_REG, EXT_UnSi,ALUSrc_IMM, DST_Rt, ALU_AND, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    LUI   -> List(F, J_XXX, Read_REG, EXT_UnSi,ALUSrc_IMM, DST_Rt, ALU_LUI, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    NOR   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_NOR, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    OR    -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_OR , F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    ORI   -> List(F, J_XXX, Read_REG, EXT_UnSi,ALUSrc_IMM, DST_Rt, ALU_OR , F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    XOR   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_XOR, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    XORI  -> List(F, J_XXX, Read_REG, EXT_UnSi,ALUSrc_IMM, DST_Rt, ALU_XOR, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),

    // Shift instruction
    SLLV  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_SLLV, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    SLL   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW,  DST_Rd, ALU_SLL, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
    SRAV  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_SRAV, F, F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F, T,F,F),
    SRA   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_SRA, F, F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F, T,F,F),
    SRLV  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_SRLV, F, F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F, T,F,F),
    SRL   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_SRL, F, F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F, T,F,F),
    
	// Branch Jump
	BEQ   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW , DST_XX, ALU_SUB, F, F, St_XX, Ld_XXX, BR_BEQ , WB_XXX, F, F, F, F,F,F),
    BNE   -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_FW , DST_XX, ALU_SUB, F, F, St_XX, Ld_XXX, BR_BNE , WB_XXX, F, F, F, F,F,F),	
    BGEZ  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_ZERO,DST_XX, ALU_SLT, F, F, St_XX, Ld_XXX, BR_BGEZ, WB_XXX, F, F, F, F,F,F),
    BGTZ  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_ZERO,DST_XX, ALU_SLT, F, F, St_XX, Ld_XXX, BR_BGTZ, WB_XXX, F, F, F, F,F,F),
    BLEZ  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_ZERO,DST_XX, ALU_SLT, F, F, St_XX, Ld_XXX, BR_BLEZ, WB_XXX, F, F, F, F,F,F),
    BLTZ  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_ZERO,DST_XX, ALU_SLT, F, F, St_XX, Ld_XXX, BR_BLTZ, WB_XXX, F, F, F, F,F,F),
    BGEZAL-> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_ZERO,DST_XX, ALU_SLT, F, F, St_XX, Ld_XXX, BR_BGEZAL,WB_PC4, F, F, T, T,F,F),
    BLTZAL-> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_ZERO,DST_XX, ALU_SLT, F, F, St_XX, Ld_XXX, BR_BLTZAL,WB_PC4, F, F, T, T,F,F),
			
    J     -> List(F, J_IMM, Read_XXX, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_XXX, F, F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F, F,F,F),
	JAL   -> List(F, J_IMM, Read_XXX, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_XXX, F, F, St_XX, Ld_XXX, BR_XXXX, WB_PC4, F, F, T, T,F,F),
	JALR  -> List(F, J_RS , Read_XXX, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_XXX, F, F, St_XX, Ld_XXX, BR_XXXX, WB_PC4, F, F, T, T,F,F),
	JR    -> List(F, J_RS , Read_XXX, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_XXX, F, F, St_XX, Ld_XXX, BR_XXXX, WB_XXX, F, F, F, F,F,F),
    
	// Data movement instruction
    MFHI  -> List(F, J_XXX, Read_HI , EXT_XXX, ALUSrc_XXX, DST_Rd, ALU_COPY_A, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
	MFLO  -> List(F, J_XXX, Read_LO , EXT_XXX, ALUSrc_XXX, DST_Rd, ALU_COPY_A, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,F,F),
	MTHI  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_COPY_A, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, T, F, F, F,F,F),
	MTLO  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_XXX, DST_XX, ALU_COPY_A, F, F, St_XX, Ld_XXX, BR_XXXX, WB_ALU, F, T, F, F,F,F),
    
	
	// Access and storage instructions
	LB    -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_Rt, ALU_ADD, F, T, St_XX, Ld_LB , BR_XXXX, WB_MEM, F, F, F, T,F,F),
	LBU   -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_Rt, ALU_ADD, F, T, St_XX, Ld_LBU, BR_XXXX, WB_MEM, F, F, F, T,F,F),
	LH    -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_Rt, ALU_ADD, F, T, St_XX, Ld_LH , BR_XXXX, WB_MEM, F, F, F, T,F,F),
	LHU   -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_Rt, ALU_ADD, F, T, St_XX, Ld_LHU, BR_XXXX, WB_MEM, F, F, F, T,F,F),
	LW    -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_Rt, ALU_ADD, F, T, St_XX, Ld_LW , BR_XXXX, WB_MEM, F, F, F, T,F,F),
    
	SB    -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_XX, ALU_ADD, T, F, St_SB , Ld_XXX, BR_XXXX, WB_XXX, F, F, F, F,F,F),
    SH    -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_XX, ALU_ADD, T, F, St_SH , Ld_XXX, BR_XXXX, WB_XXX, F, F, F, F,F,F),
    SW    -> List(F, J_XXX, Read_REG, EXT_Sign, ALUSrc_IMM, DST_XX, ALU_ADD, T, F, St_SW , Ld_XXX, BR_XXXX, WB_XXX, F, F, F, F,F,F),

	// Privileged instruction
	MFC0  -> List(F, J_XXX, Read_CP0, EXT_XXX, ALUSrc_ZERO, DST_Rt,ALU_COPY_A, F, F, St_XX , Ld_XXX, BR_XXXX, WB_ALU, F, F, F, T,T,F),
	MTC0  -> List(F, J_XXX, Read_REG, EXT_XXX, ALUSrc_XXX , DST_Rd,ALU_COPY_B, F, F, St_XX , Ld_XXX, BR_XXXX, WB_ALU, F, F, F, F,F,T)

	)

}

class CPathIo extends Bundle()
{

	val Inst 		= Input(UInt(32.W))

	//*******PC & IF/ID*******//
	val NOP         	= Output(Bool())
	val Jump        	= Output(UInt(2.W))
	val ReadData1_Src  	= Output(UInt(2.W))   	// Choice of multiplication or division
	val ExtOp       	= Output(Bool())    	// Used for Control If Extend Sign


	//*******ID/EX*******//
	val ALUSrc      = Output(UInt(2.W))	
 	val RegDst      = Output(Bool())
    val ALUOp       = Output(UInt(5.W))

	//*******EX/MEM*******//
	val MemWrite	= Output(Bool())
	val MemRead		= Output(Bool())
	val Store_type  = Output(UInt(2.W))
	val Load_type   = Output(UInt(3.W))
    val Branch_type = Output(UInt(4.W))			

   
   //*******MEM/WB*******//

    val MemtoReg    = Output(UInt(2.W))
   

	val HI_Wr     	= Output(Bool())
	val LO_Wr       = Output(Bool())
	val If31        = Output(Bool())
 	val RegWrite    = Output(Bool())

	val CP0_Read	= Output(Bool())
	val CP0_Write	= Output(Bool())
}


class Control extends Module {

	
  	val io 				= IO( new CPathIo )
  	val ControlSignals 	= ListLookup(io.Inst, Control.default, Control.map)

	//*******PC & IF/ID*******//
	io.NOP         		:= ControlSignals(0)
	io.Jump        		:= ControlSignals(1)
	io.ReadData1_Src 	:= ControlSignals(2)
	io.ExtOp       		:= ControlSignals(3)

	//*******ID/EX*******//
	io.ALUSrc       	:= ControlSignals(4)
	io.RegDst     		:= ControlSignals(5)
	io.ALUOp      		:= ControlSignals(6)

	//*******EX/MEM*******//
	io.MemWrite	  		:= ControlSignals(7)
	io.MemRead	   		:= ControlSignals(8)
	io.Store_type     	:= ControlSignals(9)
	io.Load_type     	:= ControlSignals(10)
	io.Branch_type     	:= ControlSignals(11)


    //*******MEM/WB*******//
	io.MemtoReg	   		:= ControlSignals(12)
	io.HI_Wr        	:= ControlSignals(13)
	io.LO_Wr        	:= ControlSignals(14)
	io.If31        		:= ControlSignals(15)
	io.RegWrite       	:= ControlSignals(16)

	io.CP0_Read			:= ControlSignals(17)
	io.CP0_Write		:= ControlSignals(18)

}
