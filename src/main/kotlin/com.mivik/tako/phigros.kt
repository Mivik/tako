package com.mivik.tako

import org.json.JSONArray
import org.json.JSONObject

internal const val FORMAT_VERSION = 3
internal const val RELATIVE_X_SCALE: Double = 8.9
internal const val NEG_INF: Time = -999999L
internal const val POS_INF: Time = 1000000000L

private fun toJSON(events: List<JudgeLine.Event>) = JSONArray().apply {
	fun add(
		startTime: Time, endTime: Time,
		start: Double, end: Double,
		start2: Double, end2: Double,
		interpolator: Interpolator? = null
	) {
		if (interpolator == null) {
			put(JSONObject().apply {
				put("startTime", startTime)
				put("endTime", endTime)
				put("start", start)
				put("end", end)
				put("start2", start2)
				put("end2", end2)
			})
			return
		}
		var lastValue1 = start
		var lastValue2 = start2
		for (i in (startTime + 1)..endTime) {
			val progress = interpolator((i - startTime).toDouble() / (endTime - startTime))
			val nowValue1 = start + progress * (end - start)
			val nowValue2 = start2 + progress * (end2 - start2)
			add(i - 1, i, lastValue1, nowValue1, lastValue2, nowValue2)
			lastValue1 = nowValue1
			lastValue2 = nowValue2
		}
	}
	if (events.size <= 1) {
		var value1 = 0.0
		var value2 = 0.0
		if (events.isNotEmpty()) {
			value1 = events[0].component1()
			value2 = events[0].component2()
		}
		add(NEG_INF, POS_INF, value1, value1, value2, value2)
		return@apply
	}
	val sortedEvents = events.sortedWith(compareBy({ it.endTime }, { it.startTime }))
	var lastTime = NEG_INF
	var lastValue1 = 0.0
	var lastValue2 = 0.0
	fun fillTo(toTime: Time) {
		add(lastTime, toTime, lastValue1, lastValue1, lastValue2, lastValue2)
	}
	for (event in sortedEvents) {
		val (value1, value2) = event
		if (event.isSinglePoint()) { // single point
			if (lastTime != event.endTime) {
				if (lastTime == NEG_INF) {
					lastValue1 = value1
					lastValue2 = value2
				}
				fillTo(event.endTime)
			} else {
				lastValue1 = value1
				lastValue2 = value2
			}
		} else { // linear
			assert(lastTime != NEG_INF)
			if (lastTime != event.startTime) fillTo(event.startTime)
			add(
				event.startTime, event.endTime, lastValue1, value1, lastValue2, value2,
				(event as? JudgeLine.Event.Animation)?.interpolator
			)
		}
		lastValue1 = value1
		lastValue2 = value2
		lastTime = event.endTime
	}
	fillTo(POS_INF)
}

fun JudgeLine.toPhigros() = JSONObject().apply {
	put("bpm", bpm)
	put("numOfNotes", notes.size)
	val aboveCount = notes.count { it.above }
	put("numOfNotesAbove", aboveCount)
	put("numOfNotesBelow", notes.size - aboveCount)
	put(
		"judgeLineDisappearEvents",
		toJSON(
			listOf(JudgeLine.Event.Alpha(0, 0, 0.0)) +
					events.filterIsInstance<JudgeLine.Event.Alpha>()
		)
	)
	put(
		"judgeLineMoveEvents", toJSON(
			listOf(JudgeLine.Event.Move(0, 0, 0.5, 0.5)) +
					events.mapNotNull {
						if (it !is JudgeLine.Event.Move) return@mapNotNull null
						return@mapNotNull JudgeLine.Event.Move(
							it.startTime, it.endTime,
							(it.x + 1) / 2, (it.y + 1) / 2,
							it.interpolator
						)
					})
	)
	put(
		"judgeLineRotateEvents", toJSON(
			listOf(JudgeLine.Event.Rotate(0, 0, 0.0)) +
					events.filterIsInstance<JudgeLine.Event.Rotate>()
		)
	)

	val secondsPerTime = 60.0 / bpm / 32

	class SpeedEvent(val startTime: Time, val endTime: Time, val floorPosition: Double, val speed: Double)

	val detailedSpeedEvents = mutableListOf<SpeedEvent>()
	put("speedEvents", JSONArray().apply {
		fun add(startTime: Time, endTime: Time, floorPosition: Double, speed: Double) {
			detailedSpeedEvents += SpeedEvent(startTime, endTime, floorPosition, speed)
			put(JSONObject().apply {
				put("startTime", startTime)
				put("endTime", endTime)
				put("floorPosition", floorPosition)
				put("value", speed)
			})
		}

		val speedEvents =
			listOf(JudgeLine.Event.Speed(0, 1.0)) +
					events
						.filter { it is JudgeLine.Event.Speed || it is JudgeLine.Event.TimeChange }
						.sortedBy { it.startTime }
		var lastFloorPosition = 0.0
		var lastTime: Long = 0
		var lastSpeed = 0.0
		for (event in speedEvents) {
			if (event is JudgeLine.Event.Speed) {
				if (lastTime != event.startTime) add(lastTime, event.startTime, lastFloorPosition, lastSpeed)
				lastFloorPosition += (event.startTime - lastTime) * secondsPerTime * lastSpeed
				lastSpeed = event.speed
			} else if (event is JudgeLine.Event.TimeChange) {
				if (lastTime != event.startTime) add(lastTime, event.startTime, lastFloorPosition, lastSpeed)
				lastFloorPosition += (event.startTime - lastTime + event.offset) * secondsPerTime * lastSpeed
			} else error("Impossible")
			lastTime = event.startTime
		}
		add(lastTime, POS_INF, lastFloorPosition, lastSpeed)
	})

	fun convertNotes(notes: List<Note>) = JSONArray().apply {
		var index = 0
		for (note in notes.sortedBy { it.time }) {
			while (index < detailedSpeedEvents.size && detailedSpeedEvents[index].endTime <= note.time) ++index
			assert(index != detailedSpeedEvents.size)
			val event = detailedSpeedEvents[index]
			put(JSONObject().apply {
				put("type", note.typeId)
				put("time", note.time)
				put("positionX", note.position * RELATIVE_X_SCALE)
				put("holdTime", note.holdTime)
				put("speed", note.speed)
				put(
					"floorPosition",
					event.floorPosition + (note.time - event.startTime) * secondsPerTime * event.speed
				)
			})
		}
	}
	val (notesAbove, notesBelow) = notes.partition { it.above }
	put("notesAbove", convertNotes(notesAbove))
	put("notesBelow", convertNotes(notesBelow))
}

fun Chart.toPhigros() = JSONObject().apply {
	put("formatVersion", FORMAT_VERSION)
	put("offset", -offset)
	put("numOfNotes", judgeLines.sumOf { it.notes.size })
	put("judgeLineList", JSONArray().apply {
		for (judgeLine in judgeLines)
			put(judgeLine.toPhigros())
	})
}
