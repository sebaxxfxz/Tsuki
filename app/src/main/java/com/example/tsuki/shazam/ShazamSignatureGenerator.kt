package com.example.tsuki.shazam

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

data class ShazamSignature(val uri: String, val sampleDurationMs: Long)

class ShazamSignatureGenerator(private val maxTimeSeconds: Double = 3.1, private val maxPeaks: Int = 255) {
    private val queue = IntList()
    private var consumed = 0
    private val rate = 16000
    private val engine = FftEngine(2048)
    private val hann = DoubleArray(2048) { idx ->
        val n = 2050
        val j = idx + 1
        0.5 - 0.5 * cos(2.0 * PI * j / (n - 1))
    }
    private val re = DoubleArray(2048)
    private val im = DoubleArray(2048)
    private val ring = IntArray(2048)
    private var ringIdx = 0
    private val ffts = Array(256) { DoubleArray(1025) }
    private var fftHead = 0
    private var fftCount = 0
    private val spreads = Array(256) { DoubleArray(1025) }
    private var spreadHead = 0
    private var spreadCount = 0
    private var totalSamples = 0
    private val peaksByBand = linkedMapOf<Band, MutableList<Peak>>()

    fun feedPcm16Mono(samples: ShortArray) { for (s in samples) queue.add(s.toInt()) }
    fun reset() {
        queue.clear(); consumed = 0; ring.fill(0); ringIdx = 0; fftHead = 0; fftCount = 0; spreadHead = 0; spreadCount = 0; totalSamples = 0; peaksByBand.clear()
    }

    fun nextSignatureOrNull(): ShazamSignature? {
        if (queue.size - consumed < 128) return null
        while (queue.size - consumed >= 128 && (totalSamples.toDouble() / rate < maxTimeSeconds || peaksByBand.values.sumOf { it.size } < maxPeaks)) {
            ingest(queue, consumed, consumed + 128)
            consumed += 128
        }
        if (peaksByBand.isEmpty()) return null
        val msg = Payload(rate, totalSamples, peaksByBand.toSortedMap(compareBy { it.v }).mapValues { it.value.toList() })
        val sig = ShazamSignature(msg.toUri(), (totalSamples * 1000L) / rate)
        reset()
        return sig
    }

    private fun ingest(src: IntList, from: Int, to: Int) {
        totalSamples += to - from
        var p = from
        while (p < to) { runFft(src, p, p + 128); runSpreading() ; p += 128 }
    }

    private fun runFft(src: IntList, from: Int, to: Int) {
        for (i in from until to) { ring[ringIdx] = src[i]; ringIdx = (ringIdx + 1) % ring.size }
        var rp = ringIdx
        for (i in 0 until 2048) {
            if (rp == 2048) rp = 0
            re[i] = ring[rp].toDouble() * hann[i]
            im[i] = 0.0
            rp++
        }
        engine.transform(re, im)
        val out = ffts[fftHead]
        for (k in 0..1024) {
            val rv = re[k]; val iv = im[k]; val v = (rv * rv + iv * iv) / 131072.0
            out[k] = if (v <= 1e-10) 1e-10 else v
        }
        fftHead = (fftHead + 1) % ffts.size
        fftCount++
    }

    private fun runSpreading() { spread(); if (spreadCount >= 46) recognize() }

    private fun spread() {
        val last = (fftHead - 1).modPos(ffts.size)
        val src = ffts[last]
        val curSpread = DoubleArray(1025)
        for (i in 0..1021) curSpread[i] = max(src[i], max(src[i + 1], src[i + 2]))
        curSpread[1022] = src[1022]; curSpread[1023] = src[1023]; curSpread[1024] = src[1024]
        val s1 = spreads[(spreadHead - 1).modPos(spreads.size)]
        val s2 = spreads[(spreadHead - 3).modPos(spreads.size)]
        val s3 = spreads[(spreadHead - 6).modPos(spreads.size)]
        for (b in 0 until 1025) {
            val m1 = max(curSpread[b], s1[b]); s1[b] = m1
            val m2 = max(m1, s2[b]); s2[b] = m2
            val m3 = max(m2, s3[b]); s3[b] = m3
        }
        System.arraycopy(curSpread, 0, spreads[spreadHead], 0, 1025)
        spreadHead = (spreadHead + 1) % spreads.size
        spreadCount++
    }

    private fun recognize() {
        val fft46 = ffts[(fftHead - 46).modPos(ffts.size)]
        val spr49 = spreads[(spreadHead - 49).modPos(spreads.size)]
        for (bin in 10..1014) {
            val e = fft46[bin]
            if (e < 1.0 / 64.0 || e < spr49[bin - 1]) continue
            var nb = 0.0
            for (o in intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8)) nb = max(nb, spr49[bin + o])
            if (e <= nb) continue
            var nt = nb
            for (o in intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249)) nt = max(nt, spreads[(spreadHead + o).modPos(spreads.size)][bin - 1])
            if (e <= nt) continue
            val mag = ln(max(1.0 / 64.0, e)) * 1477.3 + 6144.0
            val magL = ln(max(1.0 / 64.0, fft46[bin - 1])) * 1477.3 + 6144.0
            val magR = ln(max(1.0 / 64.0, fft46[bin + 1])) * 1477.3 + 6144.0
            val v1 = mag * 2.0 - magL - magR
            if (v1 <= 0) continue
            val v2 = (magR - magL) * 32.0 / v1
            val cBin = bin * 64.0 + v2
            val hz = cBin * (rate / 2.0 / 1024.0 / 64.0)
            val band = when {
                hz in 250.0..520.0 -> Band.B250_520
                hz > 520.0 && hz <= 1450.0 -> Band.B520_1450
                hz > 1450.0 && hz <= 3500.0 -> Band.B1450_3500
                hz > 3500.0 && hz <= 5500.0 -> Band.B3500_5500
                else -> null
            } ?: continue
            peaksByBand.getOrPut(band) { mutableListOf() }.add(Peak(spreadCount - 46, mag.toInt(), cBin.toInt()))
        }
    }

    private enum class Band(val v: Int) { B250_520(0), B520_1450(1), B1450_3500(2), B3500_5500(3) }
    private data class Peak(val fftNum: Int, val mag: Int, val bin: Int)
    private data class Payload(val hz: Int, val samples: Int, val map: Map<Band, List<Peak>>) {
        fun toUri(): String = "data:audio/vnd.shazam.sig;base64," + Base64.getEncoder().encodeToString(toBytes())
        private fun toBytes(): ByteArray {
            val out = ByteArrayOutputStream()
            for ((b, peaks) in map.entries.sortedBy { it.key.v }) {
                val pb = ByteArrayOutputStream()
                var last = 0
                for (p in peaks) {
                    var d = p.fftNum - last
                    if (d >= 255) { pb.write(0xFF); pb.writeLEInt(p.fftNum); last = p.fftNum; d = 0 }
                    pb.write(d.coerceIn(0, 254)); pb.writeLEShort(p.mag); pb.writeLEShort(p.bin); last = p.fftNum
                }
                val bytes = pb.toByteArray()
                out.writeLEInt(0x60030040 + b.v); out.writeLEInt(bytes.size); out.write(bytes)
                repeat((-bytes.size).modPos(4)) { out.write(0) }
            }
            val body = out.toByteArray()
            val sz = body.size + 8
            val hdr = ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN).apply {
                putInt(0xCAFE2580.toInt()); putInt(0); putInt(sz); putInt(0x94119C00.toInt()); putInt(0); putInt(0); putInt(0)
                putInt(3 shl 27); putInt(0); putInt(0); putInt((samples + hz * 0.24).toInt()); putInt((15 shl 19) + 0x40000)
            }
            val full = ByteArrayOutputStream(); full.write(hdr.array()); full.writeLEInt(0x40000000); full.writeLEInt(sz); full.write(body)
            val arr = full.toByteArray()
            val crc = CRC32().apply { update(arr, 8, arr.size - 8) }.value.toInt()
            ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).putInt(4, crc)
            return arr
        }
    }
}

private fun Int.modPos(m: Int): Int { val r = this % m; return if (r < 0) r + m else r }
private class IntList(cap: Int = 8192) {
    private var a = IntArray(cap)
    var size = 0; private set
    operator fun get(i: Int): Int = a[i]
    fun add(v: Int) { if (size == a.size) a = a.copyOf(a.size * 2); a[size++] = v }
    fun clear() { size = 0 }
}
private fun ByteArrayOutputStream.writeLEInt(v: Int) { write(v and 0xFF); write((v ushr 8) and 0xFF); write((v ushr 16) and 0xFF); write((v ushr 24) and 0xFF) }
private fun ByteArrayOutputStream.writeLEShort(v: Int) { write(v and 0xFF); write((v ushr 8) and 0xFF) }
private class FftEngine(private val n: Int) {
    private val cosT = DoubleArray(n / 2) { i -> cos(2.0 * PI * i / n) }
    private val sinT = DoubleArray(n / 2) { i -> sin(2.0 * PI * i / n) }
    private val rev = IntArray(n) { i -> i.revBits(Integer.numberOfTrailingZeros(n)) }
    fun transform(r: DoubleArray, im: DoubleArray) {
        for (i in 0 until n) { val j = rev[i]; if (j > i) { val tr = r[i]; r[i] = r[j]; r[j] = tr; val ti = im[i]; im[i] = im[j]; im[j] = ti } }
        var len = 2
        while (len <= n) {
            val half = len / 2; val step = n / len
            var i = 0
            while (i < n) {
                var j = 0; var k = 0
                while (j < half) {
                    val c = cosT[k]; val s = sinT[k]
                    val i1 = i + j; val i2 = i1 + half
                    val r2 = r[i2]; val im2 = im[i2]
                    val tpR = r2 * c + im2 * s; val tpI = -r2 * s + im2 * c
                    r[i2] = r[i1] - tpR; im[i2] = im[i1] - tpI; r[i1] += tpR; im[i1] += tpI
                    j++; k += step
                }
                i += len
            }
            len = len shl 1
        }
    }
}
private fun Int.revBits(bits: Int): Int { var x = this; var y = 0; repeat(bits) { y = (y shl 1) or (x and 1); x = x ushr 1 }; return y }
