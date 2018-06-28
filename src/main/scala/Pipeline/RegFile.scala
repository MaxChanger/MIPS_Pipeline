//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline RegFile Module
// 
// Jiadai Sun 
//--------------------------------------------------------------------------
//**************************************************************************

package Pipeline

import chisel3._
// import freechips.rocketchip.config.Parameters

// class RegFileIO(implicit p: Parameters)  extends CoreBundle()(p) {
//     val raddr1 = Input(UInt(5.W))
//     val raddr2 = Input(UInt(5.W))
//     val rdata1 = Output(UInt(xlen.W))
//     val rdata2 = Output(UInt(xlen.W))
//     val wen    = Input(Bool())
//     val waddr  = Input(UInt(5.W))
//     val wdata  = Input(UInt(xlen.W))
// }

class RegFileIO extends Bundle()
{

    val RegWrite    = Input(Bool())     // 写使能 控制信号

    val raddr1      = Input(UInt(5.W))  // Rs
    val raddr2      = Input(UInt(5.W))  // Rt
    val waddr       = Input(UInt(5.W))  // Rd
    val wdata       = Input(UInt(32.W)) // Wb

    val rdata1      = Output(UInt(32.W))    // 读出数据
    val rdata2      = Output(UInt(32.W))
}

class RegFile extends Module {
    val io = IO( new RegFileIO )

    val RegFile = Mem(32, UInt(32.W))
    RegFile(0)  := 0.U

    when(io.RegWrite === true.B){
        RegFile(io.waddr) := io.wdata
    }

    io.rdata1 := RegFile(io.raddr1)
    io.rdata2 := RegFile(io.raddr2)

	// printf("\nReg4:%d    Reg9:%d    Reg17:%d    Reg18:%d\n",RegFile(4),RegFile(9),RegFile(17),RegFile(18))

}
