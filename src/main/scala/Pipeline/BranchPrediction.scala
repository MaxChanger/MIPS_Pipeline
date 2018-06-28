//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline BranchPrediction Module
//
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util._
import Instructions._
import ALU._
import Control._

object StatusforBranchPrediction{

    def StrongNotTaken   = "b00".U(2.W)
    def WeaklyNotTaken   = "b01".U(2.W)
    def WeaklyTaken      = "b10".U(2.W)
    def StrongTaken      = "b11".U(2.W) 

}

import StatusforBranchPrediction._

class BranchPredictionIO extends Bundle{
    
    val reset			 = Input(Bool())
    val PC               = Input(UInt(32.W))
    val Instruction      = Input(UInt(32.W))

    val Branch_type     = Input(UInt(4.W))
	val ALUOut 	        = Input(UInt(64.W))
	val zero            = Input(Bool())

    val Branch_success   = Output(UInt(32.W))
    val Branch_number    = Output(UInt(32.W))
     
    val PredictionReslut = Output(Bool())

}


class BranchPrediction extends Module {
	val io = IO( new BranchPredictionIO )

   	val BHT 	= Mem(2048, UInt(34.W)) // pc / 4 ---- Instruction ----  status 

    val Iftaken         = Wire(Bool())
    val status_next     = Wire(UInt(2.W))
    val status_current  = RegNext(next = status_next, init = "b00".U(2.W))
    status_current      := status_next
    val Branch_success  = RegInit(0.U(32.W))
	val Branch_number   = RegInit(0.U(32.W))
	// Iftaken             := false.B


    

    val IsBranch    = Wire(Bool())
    IsBranch        := false.B

    switch(io.Branch_type){
		//BEQ
		is(BR_BEQ){
			when(io.zero){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}
		//BNE
		is(BR_BNE){
			when(!io.zero){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}
	
		//BGEZ or BGEZAL
		is(BR_BGEZ){
            when(io.zero || io.ALUOut === 0.U(32.W)){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}
		is(BR_BGEZAL){	// if(io.in1.asSInt < io.in2.asSInt)  io.ALUOut = 1
            when(io.zero || io.ALUOut === 0.U(32.W)){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}

		//BGTZ
		is(BR_BGTZ){			// if(io.in1.asSInt < io.in2.asSInt)      io.ALUOut = 1
            when( !io.zero && io.ALUOut === 0.U(32.W)){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}

		//BLEZ
		is(BR_BLEZ){
            when( io.zero && io.ALUOut === 1.U(32.W)){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}

		//BLTZ or BLTZAL
		is(BR_BLTZ){
            when( io.ALUOut === 1.U(32.W) ){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}
		is(BR_BLTZAL){
            when( io.ALUOut === 1.U(32.W) ){
                IsBranch    := true.B
			}.otherwise{
				IsBranch   	:= false.B
			}
		}

		//BRXXX
		is(BR_XXXX){
			IsBranch   	:= false.B
		}

	}
	

    when(io.reset === true.B){
        Branch_success  := 0.U
        Branch_number   := 0.U
        status_next     := 0.U
        status_current  := 0.U
    }

    when(io.reset === false.B){

    
    // def StrongNotTaken   = "b00".U(2.W)
    // def WeaklyNotTaken   = "b01".U(2.W)
    // def WeaklyTaken      = "b10".U(2.W)
    // def StrongTaken      = "b11".U(2.W) 
    when(io.Branch_type === BR_BEQ || io.Branch_type === BR_BNE  || io.Branch_type === BR_BGEZ || io.Branch_type === BR_BGEZAL  || io.Branch_type === BR_BGTZ  || io.Branch_type === BR_BLEZ  || io.Branch_type === BR_BLTZAL ){

        Iftaken         := IsBranch
        Branch_number   := Branch_number + 1.U

        when(status_current  ===  StrongNotTaken){
            // when(Iftaken === true.B){
            //     status_next := WeaklyNotTaken
            // }
            status_next :=  Mux(Iftaken, WeaklyNotTaken,StrongNotTaken)
        }.elsewhen(status_current  ===  WeaklyNotTaken){
            status_next :=  Mux(Iftaken, WeaklyTaken,StrongNotTaken)
            // when(Iftaken === true.B){
            //     status_next := WeaklyTaken
            // }
        }.elsewhen(status_current  === WeaklyTaken){
            status_next :=  Mux(Iftaken, StrongTaken,WeaklyNotTaken)
            // when(Iftaken === true.B){
            //     status_next := StrongTaken
            // }
        }.elsewhen(status_current  === StrongTaken){
            status_next :=  Mux(Iftaken, StrongTaken,WeaklyTaken)
            // when(Iftaken === true.B){
            //     status_next := StrongTaken
            // }
        }

        BHT(io.PC << 2) := Cat(io.Instruction,status_current)
    }.otherwise{
        status_next := status_current
    }

    val temp_BHT        = BHT(io.PC << 2)
    
    io.PredictionReslut := (status_current === WeaklyTaken || status_current === StrongTaken)
    
    when(io.Branch_type =/= 0.U && (Iftaken === io.PredictionReslut)){
            Branch_success  := Branch_success + 1.U
    }

    io.Branch_success   := Branch_success
    io.Branch_number    := Branch_number
    // printf("Inst:%x\n",io.Instruction)
    // printf("PredictAccuarcy = %d / %d \n",  io.Branch_success,io.Branch_number)
}
}