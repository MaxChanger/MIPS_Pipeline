//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline Top Module
// 
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
import chisel3.util._
import Control._

import chisel3.iotesters.Driver

//import utils.ercesiMIPSRunner
class TopIO extends Bundle() {

	val boot			= Input(Bool())
	val reset			= Input(Bool())

	val test_im_wr		= Input(Bool())
	val test_im_rd 		= Input(Bool())

	val test_im_addr 	= Input(UInt(32.W))
	val test_im_in 		= Input(UInt(32.W))
	val test_im_out 	= Output(UInt(32.W))

	val test_dm_wr		= Input(Bool())
	val test_dm_rd 		= Input(Bool())
	val test_dm_addr 	= Input(UInt(32.W))
	val test_dm_in 		= Input(UInt(32.W))
	val test_dm_out 	= Output(UInt(32.W))

	val MEM_WB_inst     = Output(UInt(32.W))
	val BranchPrediction_Branch_success	= Output(UInt(32.W))
	val BranchPrediction_Branch_number	= Output(UInt(32.W))
}

class Top extends Module() {
	val io		= IO(new TopIO())
	// printf("PredictAccuarcy = %d\n",  io.BranchPrediction_Branch_success)

	val dpath 	= Module(new DatPath())
	val imm 	= Mem(19399, UInt(32.W))	//s
	val dmm 	= Mem(2048, UInt(8.W))		//	
			
	dpath.io.boot	:= io.boot
	dpath.io.reset	:= io.reset
	io.MEM_WB_inst 	:= dpath.io.MEMWB_Inst	//用来检测的输出
	
	// 用来输出分支预测正确率
	io.BranchPrediction_Branch_success	:= dpath.io.BranchPrediction_Branch_success	
	io.BranchPrediction_Branch_number	:= dpath.io.BranchPrediction_Branch_number


	io.test_im_out 	:= 0.U
	io.test_dm_out 	:= 0.U
	dpath.io.Inst 	:= 0.U
	
	// 跑CPU的时候
	when (io.boot === false.B){
		dpath.io.Inst := Mux(io.boot, 0.U, imm(dpath.io.imem_addr >> 2))
		
		// printf("dpath.io.Inst:%x      ",dpath.io.Inst)
		// printf("Imm:%x \n",imm(dpath.io.imem_addr >> 2))

		// printf("dpath.io.MemWrite:%d\n\n",dpath.io.MemWrite)

		// dpath.io.dmem_datOut := dmm(dpath.io.dmem_addr >> 2)

		
		// Dmm Write
		when (dpath.io.MemWrite === true.B) {
			// dmm(dpath.io.dmem_addr >> 2) := dpath.io.dmem_datIn
			when(dpath.io.Store_type === St_SW){
				dmm(dpath.io.dmem_addr  ) 	:= dpath.io.dmem_datIn(7,0)
				dmm(dpath.io.dmem_addr+1.U) := dpath.io.dmem_datIn(15,8)
				dmm(dpath.io.dmem_addr+2.U) := dpath.io.dmem_datIn(23,16)
				dmm(dpath.io.dmem_addr+3.U) := dpath.io.dmem_datIn(31,24)
			}
			
			when(dpath.io.Store_type === St_SH){
				dmm(dpath.io.dmem_addr  ) 	:= dpath.io.dmem_datIn(7,0)
				dmm(dpath.io.dmem_addr+1.U) := dpath.io.dmem_datIn(15,8)
			}

			when(dpath.io.Store_type === St_SB){
				dmm(dpath.io.dmem_addr  ) := dpath.io.dmem_datIn(7,0)
			}
		}

		// Dmm Read
		when(dpath.io.MemRead === true.B){
			when(dpath.io.Load_type === Ld_LW){
				dpath.io.dmem_datOut := Cat(dmm(dpath.io.dmem_addr+3.U), dmm(dpath.io.dmem_addr+2.U),dmm(dpath.io.dmem_addr+1.U), dmm(dpath.io.dmem_addr))
			}
			when(dpath.io.Load_type === Ld_LH){
				val datout = dmm( dpath.io.dmem_addr+1.U)
				dpath.io.dmem_datOut := Cat(Fill( 16,datout(7) ), dmm(dpath.io.dmem_addr+1.U), dmm(dpath.io.dmem_addr))
			}
			when(dpath.io.Load_type === Ld_LB){
				val datout = dmm(dpath.io.dmem_addr)
				dpath.io.dmem_datOut := Cat(Fill( 24,datout(7) ),dmm(dpath.io.dmem_addr))
			}

			when(dpath.io.Load_type === Ld_LHU){
				dpath.io.dmem_datOut := Cat(0.U(16.W), dmm(dpath.io.dmem_addr+1.U), dmm(dpath.io.dmem_addr))
			}
			when(dpath.io.Load_type === Ld_LBU){
				dpath.io.dmem_datOut := Cat(0.U(24.W),dmm(dpath.io.dmem_addr))
			}
		}
	}

	//写入和检测的时候
	// >>2 和 <<2 为了按序号挨个存储

	when (io.boot === true.B){
		when(io.test_im_wr){
			imm(io.test_im_addr >> 2) := io.test_im_in
			dpath.io.Inst 	:= 0.U

		}
		// when(io.test_dm_wr){
		// 	// dmm(io.test_dm_addr >> 2) := io.test_dm_in

		// 	dmm(io.test_dm_addr  ) := io.test_dm_in(7,0)
		// 	dmm(io.test_dm_addr+1.U) := io.test_dm_in(15,8)
		// 	dmm(io.test_dm_addr+2.U) := io.test_dm_in(23,16)
		// 	dmm(io.test_dm_addr+3.U) := io.test_dm_in(31,24)
		// 	dpath.io.Inst 	:= 0.U
		// }		
		when(io.test_im_rd){
			io.test_im_out 	:= imm(io.test_im_addr >> 2)
			dpath.io.Inst 	:= 0.U
		}
		// when (io.test_dm_rd){
		// 	io.test_dm_out 	:= dmm(io.test_dm_addr >> 2)
		// 	io.test_dm_out := Cat(dmm(io.test_dm_addr+3.U), dmm(io.test_dm_addr+2.U),
		// 					  dmm(io.test_dm_addr+1.U), dmm(io.test_dm_addr))
		// 	dpath.io.Inst 	:= 0.U
		// }

	}
	
}
