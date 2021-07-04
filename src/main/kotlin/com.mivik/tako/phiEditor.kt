package com.mivik.tako

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal const val WIDTH = 2048
internal const val HEIGHT = 1400
internal const val SPEED_SCALE = 7

private class PhiEditorBuilder(val chart: Chart) {
	private val builder = StringBuilder()

	fun build(): String = builder.toString()

	fun line(obj: Any) {
		builder.append(obj.toString())
		builder.append('\n')
	}

	fun convertTime(time: Time) = time.toDouble() / 32

	@JvmName("addNotes")
	fun add(notes: List<Note>, judgeLineId: Int) {
		for (note in notes) {
			val desc =
				if (note is Note.Hold) "${convertTime(note.time)} ${convertTime(note.time + note.holdTime)}"
				else "${convertTime(note.time)}"
			val type = when (note) {
				is Note.Click -> 1
				is Note.Hold -> 2
				is Note.Flick -> 3
				is Note.Drag -> 4
			}
			line(
				"n$type $judgeLineId $desc ${note.position * WIDTH / 2} " +
						"${if (note.above) 1 else 2} 0"
			)
			if ((note.speed - 1).absoluteValue > 1e-8)
				line("# ${note.speed}")
		}
	}

	@JvmName("addEvents")
	fun add(events: List<JudgeLine.Event>, judgeLineId: Int, convert: (JudgeLine.Event) -> Pair<Char, String>) {
		val newEvents = events.sortedBy { it.startTime }
		for ((index, event) in newEvents.withIndex()) {
			if (index != newEvents.lastIndex &&
				event.isSinglePoint() && newEvents[index + 1].isSinglePoint() &&
				event.startTime == newEvents[index + 1].startTime
			)
				continue
			val startTime = convertTime(event.startTime)
			val endTime = convertTime(event.endTime)
			val (ch, desc) = convert(event)
			if (event.isSinglePoint())
				line("c$ch $judgeLineId $startTime $desc")
			else line("c$ch $judgeLineId $startTime $endTime $desc")
		}
	}

	init {
		line((chart.offset * 1000).roundToInt())
		line("bp 0 ${chart.bpm}")
		for ((index, judgeLine) in chart.judgeLines.withIndex()) {
			add(judgeLine.notes, index)
			add(
				listOf(JudgeLine.Event.Alpha(0, 0, 0.0)) +
						judgeLine.events.filterIsInstance<JudgeLine.Event.Alpha>(), index
			) {
				it as JudgeLine.Event.Alpha
				val alpha = (it.alpha * 255).toInt().toString()
				if (it.isSinglePoint()) Pair('a', alpha)
				else Pair('f', alpha)
			}
			add(
				listOf(JudgeLine.Event.Move(0, 0, 0.0, 0.0)) +
						judgeLine.events.filterIsInstance<JudgeLine.Event.Move>(), index
			) {
				it as JudgeLine.Event.Move
				val x = ((it.x + 1) / 2) * WIDTH
				val y = ((it.y + 1) / 2) * HEIGHT
				val desc = "$x $y"
				if (it.isSinglePoint()) Pair('p', desc)
				else Pair('m', "$desc 1")
			}
			add(
				listOf(JudgeLine.Event.Rotate(0, 0, 0.0)) +
						judgeLine.events.filterIsInstance<JudgeLine.Event.Rotate>(), index
			) {
				it as JudgeLine.Event.Rotate
				val arc = (-it.arc).toString()
				if (it.isSinglePoint()) Pair('d', arc)
				else Pair('r', "$arc 1")
			}
			add(
				(listOf(JudgeLine.Event.Speed(0, 1.0)) +
						judgeLine.events.filterIsInstance<JudgeLine.Event.Speed>()).map {
					JudgeLine.Event.Speed(it.startTime, it.speed * SPEED_SCALE)
				}, index
			) {
				Pair('v', (it as JudgeLine.Event.Speed).speed.toString())
			}
		}
	}
}

fun Chart.toPhiEditor() = PhiEditorBuilder(this).build()
