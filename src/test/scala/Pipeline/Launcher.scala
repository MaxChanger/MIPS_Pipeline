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

object Launcher {
  val tests = Map(
    
    "Top" -> { (manager: TesterOptionsManager) =>
        Driver.execute(() => new Top, manager) {
          (c) => new TopTests(c)
        }
    }
  )

  def main(args: Array[String]): Unit = {
    TutorialRunner("examples", tests, args)
  }
}



// object Launcher {
//   val tests = Map(
    
//     "Top" -> { (backendName: String) =>
//       Driver(() => new Top(), backendName) {
//         (c) => new TopTests(c)
//       }
//     }
//   )

//   def main(args: Array[String]): Unit = {
//     ercesiMIPSRunner(tests, args) 
//   }
// }
