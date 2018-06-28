// See LICENSE.txt for license details.
//**************************************************************************
//--------------------------------------------------------------------------
// ercesiMIPS Single Cycle Processor Test Launcher
//
// Meng zhang
// version 0.1

//**************************************************************************
//--------------------------------------------------------------------------
// Pipeline ALU Module
//
// Jiadai Sun  
//--------------------------------------------------------------------------
//**************************************************************************


//    test:runMain Pipeline.Launcher Top --backend-name verilator
//    ./run-examples.sh Top --backend-name verilator

package Pipeline

import chisel3._
import chisel3.iotesters.{Driver, TesterOptionsManager}
import utils.ercesiMIPSRunner
import utils.TutorialRunner

object PipelineLauncher {
  val tests = Map(
    
    "Top" -> { (backendName: String) =>
      Driver(() => new Top(), backendName) {
        (c) => new PipelineTopTests(c)
      }
    },
    "TopBP" -> { (backendName: String) =>
      Driver(() => new Top(), backendName) {
        (c) => new PiplineTopTestsBP(c)
      }
    }
  )

  def main(args: Array[String]): Unit = {
    ercesiMIPSRunner(tests, args) 
  }
}
