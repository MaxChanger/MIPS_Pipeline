//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline Branch Module
//
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util._
import Control._


class BranchIO extends Bundle{
    	val Branch_type = Input(UInt(4.W))
		val ALUOut 	    = Input(UInt(64.W))
		val zero   	    = Input(Bool())

		val PCSrc   	= Output(Bool())
		val IF_flush 	= Output(Bool())
		val ID_flush 	= Output(Bool())
		val EX_flush 	= Output(Bool())
}


class Branch extends Module {
	val io = IO( new BranchIO )

	switch(io.Branch_type){
		//BEQ
		is(BR_BEQ){
			when(io.zero){
				io.PCSrc   	:= true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc   	:= false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}
		//BNE
		is(BR_BNE){
			when(!io.zero){ //io.zero === false.B
				io.PCSrc    := true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc   	:= false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}
	
		//BGEZ or BGEZAL
		is(BR_BGEZ){
			when(io.zero || io.ALUOut === 0.U(32.W)){
				io.PCSrc    := true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc    := false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}
		is(BR_BGEZAL){	// if(io.in1.asSInt < io.in2.asSInt)  io.ALUOut = 1
			when(io.zero || io.ALUOut === 0.U(32.W)){
				io.PCSrc    := true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc    := false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}



		//BGTZ
		is(BR_BGTZ){			// if(io.in1.asSInt < io.in2.asSInt)      io.ALUOut = 1
			when( !io.zero && io.ALUOut === 0.U(32.W)){ //When io.in1.asSInt < io.in2.asSInt
				io.PCSrc    := true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc    := false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}


		//BLEZ
		is(BR_BLEZ){
			when(io.zero || io.ALUOut === 1.U(32.W)){
				io.PCSrc    := true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc    := false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}

		//BLTZ or BLTZAL
		is(BR_BLTZ){
			when(io.ALUOut === 1.U(32.W)){
				io.PCSrc    := true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc    := false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}
		is(BR_BLTZAL){
			when(io.ALUOut === 1.U(32.W)){
				io.PCSrc    := true.B
				io.IF_flush := true.B
				io.ID_flush := true.B
				// io.EX_flush := true.B
			}.otherwise{
				io.PCSrc    := false.B
				io.IF_flush := false.B
				io.ID_flush := false.B
				// io.EX_flush := false.B
			}
		}

		//BRXXX
		is(BR_XXXX){
			io.PCSrc   	:= false.B
			io.IF_flush := false.B
			io.ID_flush := false.B
			// io.EX_flush := false.B
		}

	}
	
	

	

	

}