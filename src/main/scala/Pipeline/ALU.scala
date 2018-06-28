//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline ALU Module
//
// Jiadai Sun  
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util._

object ALU {

	def ALU_AND	 	= 0.U(5.W)
	def ALU_OR 		= 1.U(5.W)
	def ALU_NOR 	= 2.U(5.W)
	def ALU_XOR 	= 3.U(5.W)

	def ALU_ADD 	= 4.U(5.W)
	def ALU_SUB 	= 5.U(5.W)

	def ALU_SLL 	= 6.U(5.W)
	def ALU_SRL 	= 7.U(5.W)
	def ALU_SRA    	= 8.U(5.W)
	def ALU_SLLV	= 9.U(5.W)
	def ALU_SRLV 	= 10.U(5.W)
	def ALU_SRAV    = 11.U(5.W)    

	def ALU_SLT 	= 12.U(5.W)
	def ALU_SLTU 	= 13.U(5.W)

	
	def ALU_LUI 	= 14.U(5.W)
	def ALU_DIV 	= 15.U(5.W)
	def ALU_DIVU 	= 16.U(5.W)

	def ALU_MULT	= 17.U(5.W)
	def ALU_MULTU	= 18.U(5.W)

	def ALU_COPY_A 	= 19.U(5.W)
	def ALU_COPY_B 	= 20.U(5.W)

	def ALU_BEQ		= 21.U(5.W)

	def ALU_XXX 	= 31.U(5.W)
	// def isSub(cmd: UInt) = (cmd === ALU_SUB) || (cmd === ALU_SLT)

}

import ALU._

class ALUIO extends Bundle{

	val ALUctr 	= Input(UInt(5.W))		// Selected operations ADD or SUB etc.
	val in1		= Input(UInt(32.W))		// op1 
	val in2 	= Input(UInt(32.W))		// op2
	val shamt 	= Input(UInt(5.W))

	val ALUOut	= Output(UInt(64.W))	// ALU Out
	val cmp_out	= Output(Bool())		// If equal -> cmp_out = 1 
}

class ALU extends Module{
	val io = IO( new ALUIO )

	// // ADD, SUB
	// val in2_inv = Mux(isSub(io.ALUctr), ~io.in2, io.in2)
	// val in1_xor_in2 = io.in1 ^ io.in2
	// val adder_out = io.in1 + in2_inv + isSub(io.ALUctr)

	// // SLT and BEQ comparation Output
	// // For BEQ, cmp_out = (in1_xor_in2 === 0.U)
	// // For SLT, cmp_out = adder_out(31) if io.in1(31) != io.in2(31)
	// // Otherwise, cmp_out = adder_out(31)
	// io.cmp_out := Mux(io.ALUctr === FN_BEQ, in1_xor_in2 === 0.U, 
	// 	Mux(io.in1(31) != io.in2(31), adder_out(31),
	// 	Mux(adder_out(31), true.B, false.B)))

	// // AND, OR, however this can also output XOR
    // val logic_out = Mux(io.ALUctr === FN_OR, in1_xor_in2, 0.U) | 
    // Mux(io.ALUctr === FN_OR || io.ALUctr === FN_AND, io.in1 & io.in2, 0.U)
    
    // val out = Mux(io.ALUctr === FN_ADD || io.ALUctr === FN_SUB, 
    // 	adder_out, logic_out)

    // io.ALUOut := out

	io.cmp_out := ( io.in1 === io.in2)

	io.ALUOut := 0.U

	switch(io.ALUctr){
		
		is(ALU_AND){
			io.ALUOut := io.in1 & io.in2
		}
		is(ALU_OR){
			io.ALUOut := io.in1 | io.in2 
		}
		is(ALU_NOR){
			io.ALUOut := ~( io.in1 | io.in2 ) 
		}
		is(ALU_XOR){
			io.ALUOut := io.in1 ^ io.in2
		}
		
		
		is(ALU_ADD){
			io.ALUOut := io.in1 + io.in2
		}
		is(ALU_SUB){
			io.ALUOut := io.in1 - io.in2
		}
		
		is(ALU_SLL){
			io.ALUOut := io.in2 << io.shamt
		}
		is(ALU_SRL){
			io.ALUOut := io.in2 >> io.shamt
		}
		is(ALU_SRA){
			io.ALUOut := ((io.in2.asSInt >> (io.shamt))).asUInt 
		}
		is(ALU_SLLV){
			io.ALUOut := io.in2 << io.in1(5,0).asUInt()
		}
		is(ALU_SRLV){
			io.ALUOut := io.in2 >> io.in1(5,0).asUInt()
		}
		is(ALU_SRAV){
			io.ALUOut := ((io.in2.asSInt >> (io.in1(5,0).asUInt)).asUInt)
		}
		
		is(ALU_SLT){
			io.ALUOut := (io.in1.asSInt < io.in2.asSInt)
		}
		is(ALU_SLTU){
			io.ALUOut := io.in1 < io.in2
		}
		
		is(ALU_LUI){
			io.ALUOut := io.in2 << 16.U
		}
		is(ALU_DIV){

			val Shang =  (((io.in1.asSInt) / (io.in2.asSInt)).asUInt)
			val YuShu =  (((io.in1.asSInt) % (io.in2.asSInt)).asUInt) 
			// HI LO in MULT and DIV is  contrary
			io.ALUOut := Cat(YuShu(31,0),Shang(31,0))		
		}
		is(ALU_DIVU){

			val YuShu = io.in1 % io.in2
			val Shang = io.in1 / io.in2
		
			io.ALUOut := Cat(YuShu(31,0),Shang(31,0))

		}
		is(ALU_MULT){
			io.ALUOut := (((io.in1.asSInt) * (io.in2.asSInt)).asUInt)
		}
		is(ALU_MULTU){
			io.ALUOut := io.in1 * io.in2
		}
		is(ALU_COPY_A){
			io.ALUOut := io.in1
		}
		is(ALU_COPY_B){
			io.ALUOut := io.in2
		}
	}
	
}
