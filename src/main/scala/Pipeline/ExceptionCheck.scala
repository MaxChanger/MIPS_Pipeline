//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline ExceptionCheck Module
// 
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util._
import Instructions._
import ALU._
import java.io.PrintWriter

// import scala.io.Source
// import scala.collection.mutable.ArrayBuffer

object ExcCode{
    
    // Used for CP0 Register
    def ExcCode_AdEL    = 4.U(4.W)  // Exception of address (read data or take instructions)
    def ExcCode_AdES    = 5.U(4.W)  // Address error (write data)
    def ExcCode_Sys     = 8.U(4.W)  // System call exception
    def ExcCode_Bp      = 9.U(4.W)  // Breakpoint exception
    def ExcCode_RI      = 10.U(4.W) // Reservation instruction exception
    def ExcCode_Ov      = 12.U(4.W) // Calculate the spillover exceptions / Overflow

    def BadVAddr        = 8.U
    def Status          = 12.U
    def Cause           = 13.U
    def EPC             = 14.U

    def asUInt(InInt: Int) = (BigInt(InInt >>> 1) << 1) + (InInt & 1)

}

import ExcCode._

class ExceptionCheckIO extends Bundle{
    

    val PC              = Input(UInt(32.W))
    val Instruction     = Input(UInt(32.W))
    val Previous_Inst   = Input(UInt(32.W))
    val in1             = Input(UInt(32.W))
    val in2             = Input(UInt(32.W))
    val ALUOut 	        = Input(UInt(64.W))

    val ExcepSrc     = Output(UInt(2.W))
    
    val IF_flush 	 = Output(Bool())
    val ID_flush 	 = Output(Bool())
	val EX_flush 	 = Output(Bool())



    val CP0_Read    = Input(Bool())
    val CP0_Write   = Input(Bool())
    val Rd          = Input(UInt(5.W))
    val Addr        = Input(UInt(5.W))
    val DataIn      = Input(UInt(32.W))

    val DataOut     = Output(UInt(32.W))
    val EPCOut      = Output(UInt(32.W))

}


class ExceptionCheck extends Module {
	val io = IO( new ExceptionCheckIO )

    val CP0     = Mem(32, UInt(32.W))

    io.ExcepSrc     := 0.U(2.W)
    io.EPCOut       := CP0(EPC) // Mux( (io.Instruction === SYSCALL), (CP0(EPC) - 4.U) ,CP0(EPC) )

    io.IF_flush     := false.B
    io.ID_flush     := false.B
    io.EX_flush     := false.B

    val IsDelayInst  = (io.Previous_Inst === BEQ    || io.Previous_Inst === BNE     ||
                        io.Previous_Inst === BGEZ   || io.Previous_Inst === BGTZ    ||
                        io.Previous_Inst === BLEZ   || io.Previous_Inst === BLTZ    ||
                        io.Previous_Inst === BGEZAL || io.Previous_Inst === BLTZAL  ||
                        io.Previous_Inst === J      || io.Previous_Inst === JAL     ||
                        io.Previous_Inst === JALR   || io.Previous_Inst === JR
                        ) 
    val IsAchieve  = (
         
        io.Instruction === ADD   ||  io.Instruction === ADDI  ||  io.Instruction === ADDU   ||  io.Instruction === ADDIU    ||
        io.Instruction === SUB   ||  io.Instruction === SUBU  ||  io.Instruction === SLT    ||  io.Instruction === SLTI     ||  io.Instruction === SLTU  ||  io.Instruction === SLTIU ||  io.Instruction === DIV    || io.Instruction ===  DIVU     ||  io.Instruction === MULT  ||  io.Instruction === MULTU ||  io.Instruction === AND    || io.Instruction ===  ANDI     ||  
        io.Instruction === LUI   ||  io.Instruction === NOR   ||  io.Instruction === OR     || io.Instruction ===  ORI      ||  
        io.Instruction === XOR   ||  io.Instruction === XORI  ||  io.Instruction === SLLV   || io.Instruction === SLL       ||
        io.Instruction === SRAV  ||  io.Instruction === SRA   ||  io.Instruction === SRLV   || io.Instruction === SRL       || 
        io.Instruction === BEQ   ||  io.Instruction === BNE   ||  io.Instruction === BGEZ   || io.Instruction === BGTZ      ||
        io.Instruction === BLEZ  ||  io.Instruction === BLTZ  ||  io.Instruction === BGEZAL || io.Instruction === BLTZAL    ||
        io.Instruction === J     ||  io.Instruction === JAL   ||  io.Instruction === JALR   || io.Instruction === JR        ||  
        io.Instruction === MFHI  ||  io.Instruction === MFLO  ||  io.Instruction === MTHI   || io.Instruction === MTLO      ||  
        io.Instruction === LB    ||  io.Instruction === LBU   ||  io.Instruction === LH     || io.Instruction === LHU       ||  
        io.Instruction === LW    ||  io.Instruction === SB    ||  io.Instruction === SH     || io.Instruction === SW        ||  
        io.Instruction === BREAK ||  io.Instruction === SYSCALL|| io.Instruction === ERET   || io.Instruction === MFC0      ||
        io.Instruction === MTC0  ||  io.Instruction === NOP  
    )

	// printf("IsAchieve=%x    %x\n",(IsAchieve),(io.Instruction))

    when(io.CP0_Read === true.B){
        switch(io.Rd){
            is(BadVAddr){
                io.DataOut := CP0(BadVAddr)
            }
            is(Status){
                io.DataOut := CP0(Status)
            } 
            is(Cause){ 
                io.DataOut := CP0(Cause)
            }
            is(EPC){ 
                io.DataOut := CP0(EPC)

            }
        }
    }


    
        when(io.Instruction === ERET){
            io.ExcepSrc     := 2.U
            io.IF_flush     := true.B
            io.ID_flush     := true.B
            io.EX_flush     := true.B
        }



        when(io.Instruction === ADD || io.Instruction === ADDI || io.Instruction === SUB){
            val a = Cat(io.in1(31),io.in1(31,0))        // 33 Bit
            val b = Cat(io.in2(31),io.in2(31,0))        // 33 Bit

            val out = Wire(UInt(33.W))

            when(io.Instruction === ADD || io.Instruction === ADDI ){
                out := a + b
            }.elsewhen(io.Instruction === SUB){
                out := a - b
            }

            when(out(32) =/= out(31)){
                io.ExcepSrc     := 1.U
                io.IF_flush     := true.B
                io.ID_flush     := true.B
                io.EX_flush     := true.B
                // when(io.CP0_Write === true.B){
                    val temp = Wire(UInt(32.W))
                        temp :=(ExcCode_Ov << 2.U)
                    CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
                    CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC ) //io.PC //- 4.U
                    CP0(Status)     := "b10".U(2.W)
                // }

            }
        }.elsewhen(io.Instruction === LH || io.Instruction === LHU){
            when(io.ALUOut(0) =/= 0.U){
                io.ExcepSrc     := 1.U
                io.IF_flush     := true.B
                io.ID_flush     := true.B
                io.EX_flush     := true.B
                CP0(BadVAddr)   := io.ALUOut
                val temp = Wire(UInt(32.W))
                    temp :=(ExcCode_AdEL << 2.U)
                CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
                CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC ) 
                CP0(Status)     := "b10".U(2.W)
            }
            
        }.elsewhen(io.Instruction === LW ){
            when(io.ALUOut(1,0) =/= 0.U){
                io.ExcepSrc     := 1.U
                io.IF_flush     := true.B
                io.ID_flush     := true.B
                io.EX_flush     := true.B
                CP0(BadVAddr)   := io.ALUOut
                val temp = Wire(UInt(32.W))
                    temp :=(ExcCode_AdEL << 2.U)
                CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
                CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC )
                CP0(Status)     := "b10".U(2.W)
            }

        }.elsewhen(io.Instruction === SH){
            when(io.ALUOut(0) =/= 0.U){
                io.ExcepSrc     := 1.U
                io.IF_flush     := true.B
                io.ID_flush     := true.B
                io.EX_flush     := true.B
                // when(io.CP0_Write === true.B){                

                    CP0(BadVAddr)   := io.ALUOut
                    val temp = Wire(UInt(32.W))
                        temp :=(ExcCode_AdES << 2.U)
                    CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
                    CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC ) 
                    CP0(Status)     := "b10".U(2.W)
                // }
            }

        }
        .elsewhen(io.Instruction === SW){
            when(io.ALUOut(1,0) =/= 0.U){
                io.ExcepSrc     := 1.U
                io.IF_flush     := true.B
                io.ID_flush     := true.B
                io.EX_flush     := true.B
                // when(io.CP0_Write === true.B){
                    CP0(BadVAddr)   := io.ALUOut
                    val temp = Wire(UInt(32.W))
                        temp :=(ExcCode_AdES << 2.U)
                    CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
                    CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC ) 
                    CP0(Status)     := "b10".U(2.W)
                // }
            }
        }
        .elsewhen(io.Instruction === BREAK){
            io.ExcepSrc     := 1.U
            io.IF_flush     := true.B
            io.ID_flush     := true.B
            io.EX_flush     := true.B               
            CP0(BadVAddr)   := io.ALUOut
            val temp = Wire(UInt(32.W))
                temp :=(ExcCode_Bp << 2.U)
            CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
            CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC ) 
            CP0(Status)     := "b10".U(2.W)

        }.elsewhen(io.Instruction === SYSCALL){
            io.ExcepSrc     := 1.U
            io.IF_flush     := true.B
            io.ID_flush     := true.B
            io.EX_flush     := true.B               
            CP0(BadVAddr)   := io.ALUOut
            val temp = Wire(UInt(32.W))
                temp :=(ExcCode_Sys << 2.U)
            CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
            CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC )
            CP0(Status)     := "b10".U(2.W)
        }.elsewhen(IsAchieve === false.B){
                io.ExcepSrc     := 1.U
                io.IF_flush     := true.B
                io.ID_flush     := true.B
                io.EX_flush     := true.B
                val temp = Wire(UInt(32.W))
                    temp :=(ExcCode_RI << 2.U)
                CP0(Cause)      := Mux(IsDelayInst === true.B, Cat(1.U(1.W),temp(30,0)),temp)
                CP0(EPC)        := Mux(IsDelayInst, (io.PC - 4.U) , io.PC )
                CP0(Status)     := "b10".U(2.W)

        }
}