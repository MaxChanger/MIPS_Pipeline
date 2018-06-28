//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline HazardCheck Module
// 
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._

class HazardCheck extends Module{
	val io = IO(new Bundle{
		val IDEX_MemRead= Input(Bool())		 
		val IDEX_Rt 	= Input(UInt(5.W))

		val IFID_Rs 	= Input(UInt(5.W))
		val IFID_Rt		= Input(UInt(5.W))

		val PCWr		= Output(Bool())	
		val IFID_Write	= Output(Bool())		
		val Control_Remove	= Output(Bool())	
	})
	

	when(io.IDEX_MemRead && ((io.IDEX_Rt === io.IFID_Rs) || (io.IDEX_Rt === io.IFID_Rt))){
		//stall the pipeline
		io.PCWr				:= false.B
		io.IFID_Write		:= false.B
		io.Control_Remove	:= true.B
	}.otherwise{
		io.PCWr				:= true.B
		io.IFID_Write		:= true.B
		io.Control_Remove	:= false.B
	}

}
