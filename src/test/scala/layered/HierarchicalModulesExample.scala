// SPDX-License-Identifier: Apache-2.0

package layered

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import firrtl.options.TargetDirAnnotation
import layered.stage.ElkStage

/**
  * This class has parameterizable widths, it will generate different hardware
  *
  * @param widthC io width
  */
//noinspection TypeAnnotation
class VizModC(widthC: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(widthC.W))
    val out = Output(UInt(widthC.W))
  })
  io.out := io.in
}

/**
  * instantiates a C of a particular size, VizModA does not generate different hardware
  * based on it's parameter
  * @param annoParam  parameter is only used in annotation not in circuit
  */
class VizModA(annoParam: Int) extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt())
    val out = Output(UInt())
  })
  val modC = Module(new VizModC(16))
  val modB = Module(new VizModB(16))
  val modB2 = Module(new VizModB(16))

  modC.io.in := io.in
  modB.io.in := io.in
  modB2.io.in := io.in
  io.out := modC.io.out + modB.io.out + modB2.io.out
}

class VizModB(widthB: Int) extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in = Input(UInt(widthB.W))
    val out = Output(UInt(widthB.W))
  })
  val modC = Module(new VizModC(widthB))
  modC.io.in := io.in
  io.out := modC.io.out
}

class TopOfVisualizer extends Module {
  //noinspection TypeAnnotation
  val io = IO(new Bundle {
    val in1 = Input(UInt(32.W))
    val in2 = Input(UInt(32.W))
    val select = Input(Bool())
    val out = Output(UInt(32.W))
    val memOut = Output(UInt(32.W))
  })
  val x = Reg(UInt(32.W))
  val y = Reg(UInt(32.W))

  val myMem = Mem(16, UInt(32.W))

  io.memOut := DontCare

  val modA = Module(new VizModA(64))
  val modB = Module(new VizModB(32))
  val modC = Module(new VizModC(48))

  when(io.select) {
    x := io.in1
    myMem(io.in1) := io.in2
  }.otherwise {
    x := io.in2
    io.memOut := myMem(io.in1)
  }

  modA.io.in := x

  y := modA.io.out + io.in2 + modB.io.out + modC.io.out
  io.out := y

  modB.io.in := x
  modC.io.in := x
}

object HierarchicalModulesExample extends App {

  val annos = Seq(
    TargetDirAnnotation("test_run_dir/visualizer"),
    ChiselGeneratorAnnotation(() => new TopOfVisualizer)
  )

  (new ElkStage).execute(Array("--serialize"), annos)
//  (new ElkStage).execute(Array("--flatten", "2"), annos)
//  (new ElkStage).execute(Array("--module-name", "VizModA"), annos)
}
