import com.mivik.tako.*
import org.junit.Test
import java.io.File

class TestMain {
	@Test
	fun main() {
		val chart = Chart.build(144) {
			offset = .115

			val acc = JudgeLine.Event.Animation.accelerate(1.8)
			val dec = JudgeLine.Event.Animation.decelerate(1.8)

			var introNearlyFinishTime: Time = 0
			var introFinishTime: Time = 0
			var dropStartTime: Time = 0
			var globalTime: Time

			judgeLine(0) {
				show()

				waitFor(steps(4))

				rotate(180.0, steps(24))

				pattern {
					stepTime = 16
					sync {
						drag
						hold.holdTime(48)
					}
					repeat(3) { drag }
				}.let { p ->
					repeat(2) {
						add(p.move(-0.3))
						add(p.move(0.3))
						add(p.move(-0.2))
						add(p.move(0.2))
					}
				}

				move(0.0, -0.7, steps(8))

				pattern {
					stepTime = 16
					sync {
						click.x(-0.2)
						click.x(0.2)
					}
					drag.x(0.3)
					drag.x(0.4)
					drag.x(0.3)
				}.flip().transformNotes { it.copy().speed(1.7) }.let { p ->
					repeat(2) {
						add(p.mirror())
						add(p)
					}
				}
				rotate(0.0)
				introNearlyFinishTime = nowTime
				hide()
			}

			judgeLine(1) {
				move(0.0, -0.7)
				nowTime = introNearlyFinishTime
				show()
				pattern {
					val time = steps(2) - 10
					sync {
						hold.holdTime(time).x(-0.5)
						hold.holdTime(time).x(0.5)
					}
					nowTime = steps(2)
				}.let { p ->
					add(p)
					add(p.transformNotes { it.copy().x(it.position * 0.6) })
				}

				hold.holdTime(steps(4) - 10)

				nowTime = introNearlyFinishTime
				val interpolator = JudgeLine.Event.Animation.decelerate(1.8)
				move(0.0, -0.6, steps(2)).interpolator(interpolator)
				waitFor(steps(2))
				move(0.0, -0.5, steps(2)).interpolator(interpolator)
				waitFor(steps(2))
				move(0.0, -0.3, steps(4)).interpolator(interpolator)
				waitFor(steps(4))
				introFinishTime = nowTime
				hide()
			}

			run {
				fun JudgeLineBuilder.fall(start: Double, time: Time) {
					move(0.0, start)
					nowTime = time
					show()
					move(0.0, start - 0.5, steps(2))
					alpha(0.0, steps(2))
				}
				judgeLine(10) { fall(-0.7, introNearlyFinishTime) }
				judgeLine(11) { fall(-0.6, introNearlyFinishTime + steps(2)) }
				judgeLine(12) { fall(-0.5, introNearlyFinishTime + steps(4)) }
			}

			// Intro finishes

			judgeLine(0) {
				move(0.0, -0.3)
				nowTime = introFinishTime
				show()

				branch {
					rotate(15.0, steps(8)).interpolator(dec)
					nowTime += steps(8)
					rotate(0.0, steps(8)).interpolator(acc)
					nowTime += steps(8)
					rotate(-15.0, steps(8)).interpolator(dec)
					nowTime += steps(8)
					rotate(0.0, steps(8))
				}

				pattern {
					stepTime = 16
					drag.x(0.3)
					drag.x(0.4)
					drag.x(0.5)
					drag.x(0.4)
				}.let { p ->
					branch {
						repeat(8) { add(p) }
						p.transformNotes { it.copy().x(-it.position) }.let { tp ->
							repeat(8) { add(tp) }
						}
					}
				}
				pattern {
					stepTime = 32
					waitFor(steps(1))
					click
					click
					hold.holdTime(steps(2) - 16)
					waitFor(steps(1))
					hold.holdTime(steps(2) - 16)
					waitFor(steps(1))
					hold.holdTime(steps(3) - 16)
					waitFor(steps(2))
					click // A
					click // way~
					waitFor(steps(4))
				}.let { p ->
					add(p.transformNotes { it.copy().x(-0.4) })
					add(p.transformNotes { it.copy().x(0.4) })
					branch {
						add(p.transformNotes { it.copy().x(0.4) })
					}
					add(p.transformNotes { it.copy().x(-0.4) })
				}
				nowTime -= steps(3)
				// For the pro-mise to come back
				stepTime = 32
				waitFor(steps(1))
				click
				click
				sync {
					hold.holdTime(48).x(0.3)
					click.x(-0.3)
				}
				waitFor(steps(1))
				sync {
					hold.holdTime(48).x(-0.3)
					click.x(0.3)
				}
				waitFor(steps(1))
				sync {
					hold.holdTime(48).x(-0.3)
					hold.holdTime(48).x(0.3)
				}
				waitFor(steps(1))
				sync {
					hold.holdTime(32).x(-0.3)
					hold.holdTime(32).x(0.3)
				}
				waitFor(16)
				hold.holdTime(steps(3) - 16)
				waitFor(steps(1) + 16)
				alpha(0.0, 16)

				speed(1.3)
				// fall
				waitFor(16)
				move(0.0, 1.5)
				move(0.0, -0.3, steps(3))
				rotate(360.0, steps(3)).interpolator(JudgeLine.Event.Animation.decelerate(1.8))
				alpha(1.0, steps(3))
				waitFor(steps(3) + 16)
				rotate(0.0)
				stepTime = 16
				pattern {
					stepTime = 16
					sync {
						click.x(-0.2)
						click.x(0.2)
					}
					drag.x(0.3)
					drag.x(0.4)
					drag.x(0.3)
				}.let { p ->
					repeat(4) {
						add(p.mirror())
						add(p)
					}
				}
				stepTime = 32
				// oh there
				repeat(2) {
					sync {
						click.x(-0.3)
						click.x(0.3)
					}
				}
				// must~ be
				branch {
					hold.holdTime(steps(6) - 16).x(-0.3)
				}
				branch {
					hold.holdTime(steps(3) - 16).x(0.3)
					waitFor(steps(2))
					hold.holdTime(steps(3) - 16).x(0.3)
				}
				repeat(6) { drag.x(-0.3) }
				// something
				move(0.0, -0.4, steps(1))
				click.speed(1.4)
				move(0.0, -0.5, steps(1))
				click.speed(1.4)
				// wrong~ with me
				branch {
					hold.holdTime(steps(3) - 16)
				}
				repeat(3) { drag }
				stepTime = 16
				repeat(2) {
					sync {
						click.x(-0.3)
						click.x(0.3)
					}
				}
				stepTime = 32
				waitFor(steps(4))
				speed(2.0)
				stepTime = 16
				repeat(8) {
					var time = nowTime
					judgeLine(10 + it * 2) {
						nowTime = time
						move(0.0, -0.5)
						show()
						move(0.0, -1.0, steps(4))
						alpha(0.0, steps(4))
					}
					sync {
						click.x(-0.35)
						click.x(0.35)
					}
					click.x(-0.3)
					time = nowTime
					judgeLine(10 + it * 2 + 1) {
						nowTime = time
						move(0.0, -0.5)
						show()
						move(0.0, -1.0, steps(4))
						alpha(0.0, steps(4))
					}
					sync {
						click.x(-0.35)
						click.x(0.35)
					}
					click.x(0.3)
				}
				stepTime = 32
				repeat(8) {
					val time = nowTime
					judgeLine(10 + it) {
						nowTime = time
						move(0.0, -0.5)
						show()
						move(0.0, -1.0, steps(4))
						alpha(0.0, steps(4))
					}
					sync {
						click.x(-0.35)
						click.x(0.35)
					}
				}
				stepTime = 16
				repeat(4) {
					click.x(0.3)
					click.x(-0.3)
				}
				stepTime = 8
				move(0.0, 0.0, steps(16))
				repeat(8) {
					click.x(0.3 - it * 0.02)
					click.x(-0.3 + it * 0.02)
				}
				alpha(0.0, steps(2))
				speed(1.0)
				dropStartTime = nowTime
			}

			globalTime = dropStartTime
			repeat(3) {
				judgeLine(it * 3 + 1) {
					move(5.0, 5.0)
					rotate(180.0)
					nowTime = globalTime - 16
					move(0.0, 0.0)
					alpha(1.0, 16)
					nowTime = globalTime
					stepTime = 32
					rotate(-10.0, 40).interpolator(dec)
					hold.holdTime(16).speed(6.0).above(false)
					stepTime = 8
					click.x(-0.3).speed(2.4)
					click.x(0.3).speed(2.4)
					click.x(-0.3).speed(2.4)
					alpha(0.0, 16)
					waitFor(steps(1))
					globalTime = nowTime
				}
				judgeLine(it * 3 + 2) {
					move(5.0, 5.0)
					rotate(-180.0)
					nowTime = globalTime - 16
					move(0.0, 0.0)
					alpha(1.0, 16)
					nowTime = globalTime
					stepTime = 32
					rotate(10.0, 40).interpolator(dec)
					hold.holdTime(16).speed(6.0).above(false)
					stepTime = 8
					click.x(0.3).speed(2.4)
					click.x(-0.3).speed(2.4)
					click.x(0.3).speed(2.4)
					alpha(0.0, 16)
					waitFor(steps(1))
					globalTime = nowTime
				}
				judgeLine(it * 3 + 3) {
					move(5.0, 5.0)
					rotate(180.0)
					nowTime = globalTime - 16
					move(0.0, 0.0)
					alpha(1.0, 16)
					nowTime = globalTime
					stepTime = 32
					rotate(0.0, 40).interpolator(dec)
					hold.holdTime(16).speed(6.0).above(false)
					stepTime = 8
					click.x(-0.3).speed(2.4)
					click.x(0.3).speed(2.4)
					click.x(-0.3).speed(2.4)
					waitFor(steps(3))
					stepTime = 32
					move(0.0, -0.3, 16).interpolator(dec)
					sync {
						click.x(-0.3).speed(3.4)
						click.x(0.3).speed(3.4)
					}
					move(0.0, -0.45, 16).interpolator(acc)
					alpha(0.0, 32)
					sync {
						click.x(-0.3).speed(3.4)
						click.x(0.3).speed(3.4)
					}
					globalTime = nowTime - 16
				}
			}

			judgeLine(0) {
				move(0.0, -0.2)
				rotate(0.0)
				nowTime = globalTime - 16
				alpha(1.0, 16)
				nowTime = globalTime
				stepTime = 32
				repeat(2) {
					move(0.0, 0.3, steps(1)).interpolator(dec)
					sync {
						hold.holdTime(32).x(-0.35)
						hold.holdTime(32).x(0.35)
					}
					sync {
						flick.above(false).x(-0.35)
						flick.above(false).x(0.35)
					}
					move(0.0, if (it == 1) -0.7 else -0.3, steps(1)).interpolator(dec)
					sync {
						hold.holdTime(32).above(false).x(-0.35)
						hold.holdTime(32).above(false).x(0.35)
					}
					sync {
						flick.x(-0.35)
						flick.x(0.35)
					}
				}
				globalTime = nowTime
			}

			repeat(64) { index ->
				judgeLine(10 + (index % 8)) {
					nowTime = globalTime + (index - 4) * 32
					rotate(0.0)
					move(0.0, 1.0)
					alpha(0.3)
					stepTime = 32
					move(0.0, -1.2, steps(4))
					repeat(4) {
						rotate(if ((it + index) % 2 == 1) -2.0 else 2.0, steps(1)).interpolator(dec)
						waitFor(steps(1))
					}
				}
			}

			judgeLine(0) {
				speed(1.5)
				pattern {
					pattern {
						pattern {
							stepTime = 16
							repeat(2) { click.x(-0.3) }
							stepTime = 8
							click.x(-0.3)
							click.x(0.0)
							click.x(0.3)
							waitFor(steps(1))
						}.let { p ->
							add(p)
							add(p.mirror())
						}
						stepTime = 16
						click.x(-0.3)
						click.x(-0.3)
						flick.x(-0.3)
						stepTime = 8
						repeat(2) {
							click.x(0.4)
							click.x(-0.4)
						}
						click.x(0.4)
						waitFor(steps(1))
						stepTime = 16
						repeat(2) {
							sync {
								click.x(-0.4)
								click.x(0.4)
							}
						}
					}.let { p ->
						add(p)
						add(p.mirror())
						add(p)
					}
					stepTime = 32
					sync {
						click.x(-0.4)
						click.x(0.4)
					}
					repeat(4) {
						move(0.0, -0.7 + (it + 1) * (1.4 / 4), steps(1))
						sync {
							hold.holdTime(16).x(-0.4)
							hold.holdTime(16).x(0.4)
						}
						sync {
							flick.x(-0.4)
							flick.x(0.4)
						}
					}
					nowTime -= 32
				}.let { p ->
					add(p)
					var first = true
					add(p.flip().transformNotes {
						if (first) null.also { first = false }
						else it
					})
				}
				speed(1.0)
			}
		}
		println(chart.toPhigros.toString())
		println(chart.toPhiEditor())
	}
}
