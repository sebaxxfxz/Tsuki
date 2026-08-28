package com.example.tsuki.together

internal sealed interface TogetherGuestOp {
    data class Control(val action: ControlAction) : TogetherGuestOp
    data class AddTrack(val track: TogetherTrack, val mode: AddTrackMode) : TogetherGuestOp
}

internal object TogetherGuestPlaybackPlanner {
    fun planPlayTrackNow(roomState: TogetherRoomState, track: TogetherTrack, positionMs: Long, playWhenReady: Boolean): List<TogetherGuestOp> {
        if (!roomState.settings.allowGuestsToControlPlayback) return emptyList()
        val id = track.id.trim()
        if (id.isEmpty()) return emptyList()
        val pos = positionMs.coerceAtLeast(0L)
        val wantsPlay = playWhenReady && !roomState.isPlaying
        val inQueue = roomState.queue.any { it.id == id }
        if (inQueue) {
            return buildList {
                add(TogetherGuestOp.Control(ControlAction.SeekToTrack(id, pos)))
                if (wantsPlay) add(TogetherGuestOp.Control(ControlAction.Play))
            }
        }
        if (!roomState.settings.allowGuestsToAddTracks) return emptyList()
        return buildList {
            add(TogetherGuestOp.AddTrack(track.copy(id = id), AddTrackMode.PLAY_NEXT))
            add(TogetherGuestOp.Control(ControlAction.SeekToTrack(id, 0L)))
            if (wantsPlay) add(TogetherGuestOp.Control(ControlAction.Play))
        }
    }
}
