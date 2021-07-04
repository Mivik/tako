package com.mivik.tako

import kotlin.math.cos
import kotlin.math.pow

typealias Time = Long
typealias Interpolator = (Double) -> Double

sealed class Note {
	var above = true
		private set
	var position = 0.0
		private set
	var speed = 1.0
		private set
	var time: Time = 0
		private set

	open val holdTime: Time
		get() = 0
	abstract val typeId: Int
	abstract fun copy(): Note

	fun above(flag: Boolean) = apply { above = flag }
	fun position(value: Double) = apply { position = value }
	fun speed(value: Double) = apply { speed = value }
	fun time(value: Time) = apply { time = value }
	fun x(value: Double) = position(value)

	class Click : Note() {
		override val typeId: Int
			get() = 1

		override fun copy() = Click().also {
			it.above(above)
			it.position(position)
			it.speed(speed)
			it.time(time)
		}
	}

	class Drag : Note() {
		override val typeId: Int
			get() = 2

		override fun copy() = Drag().also {
			it.above(above)
			it.position(position)
			it.speed(speed)
			it.time(time)
		}
	}

	class Flick : Note() {
		override val typeId: Int
			get() = 4

		override fun copy() = Flick().also {
			it.above(above)
			it.position(position)
			it.speed(speed)
			it.time(time)
		}
	}

	class Hold : Note() {
		override var holdTime: Time = 0

		override val typeId: Int
			get() = 3

		fun holdTime(value: Time) = apply { holdTime = value }

		override fun copy() = Hold().also {
			it.above(above)
			it.position(position)
			it.speed(speed)
			it.time(time)
			it.holdTime(holdTime)
		}
	}
}

class JudgeLine(var bpm: Int) {
	companion object {
		inline fun build(bpm: Int, block: JudgeLineBuilder.() -> Unit) =
			JudgeLineBuilder(bpm).apply(block).build()
	}

	sealed class Event(
		val startTime: Time,
		val endTime: Time
	) {
		abstract operator fun component1(): Double
		open operator fun component2(): Double = 0.0
		abstract fun copy(offset: Time = 0): Event

		fun isSinglePoint() = startTime == endTime

		abstract class Animation(
			startTime: Time,
			endTime: Time,
			var interpolator: Interpolator? = null
		) : Event(startTime, endTime) {
			companion object {
				fun accelerate(factor: Double = 1.0): Interpolator = { it.pow(factor * 2) }
				fun decelerate(factor: Double = 1.0): Interpolator = { 1 - (1 - it).pow(factor * 2) }
				fun accelerateDecelerate(): Interpolator = { cos((it + 1) * Math.PI) / 2 + 0.5 }

				fun bounce(): Interpolator = {
					val t = it * 1.1226
					fun fall(t: Double) = t * t * 8
					when {
						t < 0.3535 -> fall(t)
						t < 0.7408 -> fall(t - 0.54719) + 0.7
						t < 0.9644 -> fall(t - 0.8526) + 0.9
						else -> fall(t - 1.0435) + 0.95
					}
				}
			}

			fun interpolator(interpolator: Interpolator?) = apply { this.interpolator = interpolator }
		}

		class Alpha(
			startTime: Time, endTime: Time,
			val alpha: Double,
			interpolator: Interpolator? = null
		) : Animation(startTime, endTime, interpolator) {
			override operator fun component1() = alpha

			override fun copy(offset: Time) =
				Alpha(startTime + offset, endTime + offset, alpha, interpolator)
		}

		class Move(
			startTime: Time, endTime: Time,
			val x: Double,
			val y: Double,
			interpolator: Interpolator? = null
		) : Animation(startTime, endTime, interpolator) {
			override operator fun component1() = x
			override operator fun component2() = y

			override fun copy(offset: Time) =
				Move(startTime + offset, endTime + offset, x, y, interpolator)
		}

		class Rotate(
			startTime: Time, endTime: Time,
			val arc: Double,
			interpolator: Interpolator? = null
		) : Animation(startTime, endTime, interpolator) {
			override operator fun component1() = arc

			override fun copy(offset: Time) =
				Rotate(startTime + offset, endTime + offset, arc, interpolator)
		}

		class Speed(
			time: Time,
			val speed: Double
		) : Event(time, time) {
			override operator fun component1() = speed

			override fun copy(offset: Time) = Speed(startTime + offset, speed)
		}

		class TimeChange(
			time: Time,
			val offset: Time
		) : Event(time, time) {
			override operator fun component1() = 0.0

			override fun copy(offset: Time) = TimeChange(startTime + offset, this.offset)
		}
	}

	val notes = mutableListOf<Note>()
	val events = mutableListOf<Event>()
}

class Pattern(
	val notes: List<Note>,
	val events: List<JudgeLine.Event>,
	val totalTime: Time
) {
	inline fun transform(
		noteBlock: (Note) -> Note?,
		eventBlock: (JudgeLine.Event) -> JudgeLine.Event?
	) =
		Pattern(notes.mapNotNull(noteBlock), events.mapNotNull(eventBlock), totalTime)

	@Suppress("MoveLambdaOutsideParentheses")
	inline fun transformNotes(block: (Note) -> Note?) = transform(block, { it })

	inline fun transformEvents(block: (JudgeLine.Event) -> JudgeLine.Event?) = transform({ it }, block)

	fun offset(offset: Time) = transform(
		{ it.copy().time(it.time + offset) },
		{ it.copy(offset) }
	)

	fun move(x: Double) = transformNotes { it.copy().x(it.position - x) }
	fun move(x: Double, y: Double) = transformEvents {
		if (it is JudgeLine.Event.Move) JudgeLine.Event.Move(it.startTime, it.endTime, it.x + x, it.y + y)
		else it.copy()
	}

	fun flip() = transform({ it.copy().above(!it.above) }, {
		if (it is JudgeLine.Event.Move) JudgeLine.Event.Move(it.startTime, it.endTime, it.x, -it.y)
		else it.copy()
	})

	fun mirror() = transformNotes { it.copy().x(-it.position) }

	fun addTo(builder: JudgeLineBuilder) {
		builder.judgeLine.notes.addAll(notes)
		builder.judgeLine.events.addAll(events)
	}
}

class JudgeLineBuilder(val judgeLine: JudgeLine) {
	constructor(bpm: Int) : this(JudgeLine(bpm))

	var nowTime: Time = 0
	var stepTime: Time = 32
	var bpm: Int
		get() = judgeLine.bpm
		set(bpm: Int) {
			judgeLine.bpm = bpm
		}

	fun build() = judgeLine

	fun nextTime() = nowTime.also { nowTime += stepTime }

	fun add(note: Note) {
		judgeLine.notes += note.time(nextTime())
	}

	fun place(note: Note) {
		judgeLine.notes += note.time(nowTime)
	}

	fun add(event: JudgeLine.Event) {
		judgeLine.events += event
	}

	fun add(pattern: Pattern) {
		pattern.offset(nowTime).addTo(this)
		nowTime += pattern.totalTime
	}

	fun place(pattern: Pattern) {
		pattern.offset(nowTime).addTo(this)
	}

	val click: Note.Click
		inline get() = Note.Click().also { add(it) }
	val drag: Note.Drag
		inline get() = Note.Drag().also { add(it) }
	val flick: Note.Flick
		inline get() = Note.Flick().also { add(it) }
	val hold: Note.Hold
		inline get() = Note.Hold().also { add(it) }

	fun alpha(alpha: Double, duration: Time = 0) =
		JudgeLine.Event.Alpha(nowTime, nowTime + duration, alpha).also { add(it) }

	fun move(x: Double, y: Double, duration: Time = 0) =
		JudgeLine.Event.Move(nowTime, nowTime + duration, x, y).also { add(it) }

	fun rotate(arc: Double, duration: Time = 0) =
		JudgeLine.Event.Rotate(nowTime, nowTime + duration, arc).also { add(it) }

	fun speed(speed: Double) = JudgeLine.Event.Speed(nowTime, speed).also { add(it) }

	fun steps(steps: Time) = steps * stepTime

	fun hide() = alpha(0.0)
	fun show() = alpha(1.0)

	fun waitFor(time: Time) {
		nowTime += time
	}

	inline fun sync(block: JudgeLineBuilder.() -> Unit) {
		val old = stepTime
		stepTime = 0
		block()
		stepTime = old
		nextTime()
	}

	inline fun branch(block: JudgeLineBuilder.() -> Unit) {
		val oldTime = nowTime
		val oldStepTime = stepTime
		block()
		nowTime = oldTime
		stepTime = oldStepTime
	}
}

class Chart(
	val bpm: Int,
	val offset: Double,
	val judgeLines: List<JudgeLine>
) {
	companion object {
		inline fun build(bpm: Int, block: ChartBuilder.() -> Unit) = ChartBuilder(bpm).apply(block).build()
	}
}

class ChartBuilder(var bpm: Int) {
	var offset = 0.0
	private val judgeLines = mutableListOf<JudgeLine>()
	private val judgeLineTimes = mutableListOf<Time>()

	inline fun pattern(block: JudgeLineBuilder.() -> Unit): Pattern {
		val builder = JudgeLineBuilder(bpm).apply(block)
		val judgeLine = builder.judgeLine
		return Pattern(judgeLine.notes, judgeLine.events, builder.nowTime)
	}

	fun judgeLine(index: Int, block: JudgeLineBuilder.() -> Unit): JudgeLine {
		while (judgeLines.size <= index) {
			judgeLines += JudgeLine(bpm)
			judgeLineTimes += 0
		}
		return judgeLines[index].also {
			judgeLineTimes[index] =
				JudgeLineBuilder(it).apply { nowTime = judgeLineTimes[index] }.apply(block).nowTime
		}
	}

	fun judgeLine(block: JudgeLineBuilder.() -> Unit) = judgeLine(judgeLines.size, block)

	fun build() = Chart(bpm, offset, judgeLines)
}
