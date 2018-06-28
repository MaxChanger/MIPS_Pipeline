//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline Forward Module
// 
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util._

class ForwardIO extends Bundle{

    val IDEX_Rs    		= Input(UInt(32.W))
	val IDEX_Rt    		= Input(UInt(32.W))
    val EXMEM_reg_indx  = Input(UInt(32.W))
    val EXMEM_RegWrite 	= Input(Bool())
    val MEMWB_reg_indx  = Input(UInt(32.W))
    val MEMWB_RegWrite 	= Input(Bool())
        
	val EXMEM_HI_Wr	 	= Input(Bool())
	val EXMEM_LO_Wr	 	= Input(Bool())
	val MEMWB_HI_Wr	 	= Input(Bool())
	val MEMWB_LO_Wr	 	= Input(Bool())

    val IDEX_HI_Read 	= Input(Bool())
    val IDEX_LO_Read 	= Input(Bool())

	val EXMEM_CP0_Wr	= Input(Bool())
	val MEMWB_CP0_Wr	= Input(Bool())

    val IDEX_CP0_Read 	= Input(Bool())

    val ForwardA    	= Output(UInt(2.W))
    val ForwardB    	= Output(UInt(2.W))

}

class Forward extends Module{
    val io = IO(new ForwardIO )

	//********************** ForwardA **********************//
	io.ForwardA := "b00".U(2.W)			// forward --- IDEX_ReadData1


	when((io.MEMWB_RegWrite	
    && (io.MEMWB_reg_indx =/= 0.U) 
	&& ~(io.EXMEM_RegWrite && (io.EXMEM_reg_indx =/= 0.U)  && (io.EXMEM_RegWrite === io.IDEX_Rs))
	//&& ~(io.EXMEM_RegWrite && (io.EXMEM_reg_indx =/= 0.U)  && (io.EXMEM_RegWrite =/= io.IDEX_Rs))
	//&& (io.EXMEM_reg_indx != io.IDEX_Rs)
	&& (io.MEMWB_reg_indx === io.IDEX_Rs))
	|| (io.MEMWB_HI_Wr  && (io.MEMWB_HI_Wr === io.IDEX_HI_Read))
	|| (io.MEMWB_LO_Wr  && (io.MEMWB_LO_Wr === io.IDEX_LO_Read))
	|| (io.MEMWB_CP0_Wr  && (io.MEMWB_CP0_Wr === io.IDEX_CP0_Read))){
		io.ForwardA := "b01".U(2.W)		// forward --- Reg_WriteData
	}


	when((io.EXMEM_RegWrite 
    && (io.EXMEM_reg_indx =/= 0.U) 
    && (io.EXMEM_reg_indx === io.IDEX_Rs))
	|| (io.EXMEM_HI_Wr  && (io.EXMEM_HI_Wr === io.IDEX_HI_Read))		// Used for HI Foward
	|| (io.EXMEM_LO_Wr  && (io.EXMEM_LO_Wr === io.IDEX_LO_Read))	   	// Used for Lo Foward
	|| (io.EXMEM_CP0_Wr  && (io.EXMEM_CP0_Wr === io.IDEX_CP0_Read))){  	// Used for CP0 Foward
		io.ForwardA := "b10".U(2.W)		// forward --- EXMEM_ALUOut
	}





	//********************** ForwardB **********************//
	
	// The two order affects the result of the Forwarding --- Should keep the latest, That is -- EXMEM_ALUOut
	// 

	io.ForwardB := "b00".U(2.W)			// forward --- IDEX_ReadData2

	when(io.MEMWB_RegWrite 
    && (io.MEMWB_reg_indx =/= 0.U) 
	&& ~(io.EXMEM_RegWrite && (io.EXMEM_reg_indx =/= 0.U)  && (io.EXMEM_RegWrite === io.IDEX_Rt))
	//&& ~(io.EXMEM_RegWrite && (io.EXMEM_reg_indx =/= 0.U)  && (io.EXMEM_RegWrite =/= io.IDEX_Rt))
	//&& (io.EXMEM_reg_indx != io.IDEX_Rt)
	&& (io.MEMWB_reg_indx === io.IDEX_Rt)){
		io.ForwardB := "b01".U(2.W)		// forward --- Reg_WriteData
	}

	
	when(io.EXMEM_RegWrite 
    && (io.EXMEM_reg_indx =/= 0.U) 
    && (io.EXMEM_reg_indx === io.IDEX_Rt)){
		io.ForwardB := "b10".U(2.W)		// forward --- EXMEM_ALUOut
	}

}