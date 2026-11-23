package com.example.services

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.min

/**
 * Pure Kotlin QR Code (Byte mode, ECC Level L, Versions 1..10) generator
 * so URL QR sharing renders crisp, scannable QR codes offline with zero dependencies.
 */
object QrCodeGenerator {

    private val DATA_CAPACITY_BYTES_L = intArrayOf(
        0, 17, 32, 53, 78, 106, 134, 154, 192, 230, 271
    )
    private val ECC_CODEWORDS_L = intArrayOf(
        0, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18
    )
    private val BLOCK_COUNT_L = intArrayOf(
        0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4
    )
    private val ALIGNMENT_CENTERS = arrayOf(
        intArrayOf(),
        intArrayOf(),
        intArrayOf(6, 18),
        intArrayOf(6, 22),
        intArrayOf(6, 26),
        intArrayOf(6, 30),
        intArrayOf(6, 34),
        intArrayOf(6, 22, 38),
        intArrayOf(6, 24, 42),
        intArrayOf(6, 26, 46),
        intArrayOf(6, 28, 50)
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    fun generateMatrix(text: String): Array<BooleanArray> {
        val payload = text.toByteArray(Charsets.UTF_8).let {
            if (it.size > DATA_CAPACITY_BYTES_L[10]) it.copyOf(DATA_CAPACITY_BYTES_L[10]) else it
        }
        val version = (1..10).firstOrNull { payload.size <= DATA_CAPACITY_BYTES_L[it] } ?: 10
        val size = version * 4 + 17
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        placeFinderPatterns(matrix, reserved, size)
        placeAlignmentPatterns(matrix, reserved, version)
        placeTimingPatterns(matrix, reserved, size)
        reserveFormatInfo(reserved, size)
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        val codewords = buildCodewords(payload, version)
        placeDataBits(matrix, reserved, size, codewords)

        // Evaluate masks 0..7 and select best penalty score
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        for (mask in 0..7) {
            val candidate = Array(size) { r -> matrix[r].clone() }
            applyMask(candidate, reserved, size, mask)
            applyFormatInfo(candidate, size, mask)
            val penalty = scorePenalty(candidate, size)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
            }
        }

        applyMask(matrix, reserved, size, bestMask)
        applyFormatInfo(matrix, size, bestMask)
        return matrix
    }

    fun generateBitmap(text: String, pixelSize: Int = 512): Bitmap {
        val matrix = generateMatrix(text)
        val n = matrix.size
        val quiet = 4
        val totalModules = n + quiet * 2
        val scale = (pixelSize / totalModules).coerceAtLeast(1)
        val dim = totalModules * scale
        val pixels = IntArray(dim * dim) { Color.WHITE }

        for (r in 0 until n) {
            for (c in 0 until n) {
                if (matrix[r][c]) {
                    val startY = (r + quiet) * scale
                    val startX = (c + quiet) * scale
                    for (dy in 0 until scale) {
                        val rowOffset = (startY + dy) * dim + startX
                        for (dx in 0 until scale) {
                            pixels[rowOffset + dx] = Color.BLACK
                        }
                    }
                }
            }
        }
        return Bitmap.createBitmap(pixels, dim, dim, Bitmap.Config.ARGB_8888)
    }

    private fun placeFinderPatterns(m: Array<BooleanArray>, res: Array<BooleanArray>, size: Int) {
        val origins = arrayOf(0 to 0, 0 to (size - 7), (size - 7) to 0)
        for ((sr, sc) in origins) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = sr + dr
                    val c = sc + dc
                    if (r !in 0 until size || c !in 0 until size) continue
                    val on = dr in 0..6 && dc in 0..6 && (
                        dr == 0 || dr == 6 || dc == 0 || dc == 6 || (dr in 2..4 && dc in 2..4)
                        )
                    m[r][c] = on
                    res[r][c] = true
                }
            }
        }
    }

    private fun placeAlignmentPatterns(m: Array<BooleanArray>, res: Array<BooleanArray>, version: Int) {
        val centers = ALIGNMENT_CENTERS[version]
        for (r in centers) {
            for (c in centers) {
                if (res[r][c]) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val on = kotlin.math.max(kotlin.math.abs(dr), kotlin.math.abs(dc)) != 1
                        m[r + dr][c + dc] = on
                        res[r + dr][c + dc] = true
                    }
                }
            }
        }
    }

    private fun placeTimingPatterns(m: Array<BooleanArray>, res: Array<BooleanArray>, size: Int) {
        for (i in 8 until size - 8) {
            val bit = (i % 2 == 0)
            if (!res[6][i]) {
                m[6][i] = bit
                res[6][i] = true
            }
            if (!res[i][6]) {
                m[i][6] = bit
                res[i][6] = true
            }
        }
    }

    private fun reserveFormatInfo(res: Array<BooleanArray>, size: Int) {
        for (i in 0..8) {
            res[8][i] = true
            res[i][8] = true
        }
        for (i in size - 8 until size) {
            res[8][i] = true
            res[i][8] = true
        }
    }

    private fun buildCodewords(payload: ByteArray, version: Int): ByteArray {
        val totalDataBytes = when (version) {
            1 -> 19; 2 -> 34; 3 -> 55; 4 -> 80; 5 -> 108
            6 -> 136; 7 -> 156; 8 -> 194; 9 -> 232; else -> 274
        }
        val bits = ArrayList<Boolean>(totalDataBytes * 8)
        fun appendBits(value: Int, count: Int) {
            for (b in count - 1 downTo 0) {
                bits.add(((value shr b) and 1) != 0)
            }
        }

        // Byte mode indicator 0100 + count (8 bits for versions 1..9, 16 bits for version 10+)
        appendBits(0b0100, 4)
        val countBits = if (version <= 9) 8 else 16
        appendBits(payload.size, countBits)
        for (byte in payload) {
            appendBits(byte.toInt() and 0xFF, 8)
        }
        val capBits = totalDataBytes * 8
        val terminatorLen = min(4, capBits - bits.size)
        appendBits(0, terminatorLen)
        while (bits.size % 8 != 0) bits.add(false)

        val padBytes = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bits.size < capBits) {
            appendBits(padBytes[padIdx % 2], 8)
            padIdx++
        }

        val dataBytes = ByteArray(totalDataBytes) { idx ->
            var v = 0
            for (b in 0 until 8) {
                v = (v shl 1) or if (bits[idx * 8 + b]) 1 else 0
            }
            v.toByte()
        }

        val numBlocks = BLOCK_COUNT_L[version]
        val eccPerBlock = ECC_CODEWORDS_L[version]
        val shortBlockLen = totalDataBytes / numBlocks
        val longBlocks = totalDataBytes % numBlocks

        val dataBlocks = ArrayList<ByteArray>(numBlocks)
        val eccBlocks = ArrayList<ByteArray>(numBlocks)
        var offset = 0
        for (b in 0 until numBlocks) {
            val len = shortBlockLen + if (b >= numBlocks - longBlocks) 1 else 0
            val block = dataBytes.copyOfRange(offset, offset + len)
            offset += len
            dataBlocks.add(block)
            eccBlocks.add(computeReedSolomon(block, eccPerBlock))
        }

        val result = ArrayList<Byte>(totalDataBytes + numBlocks * eccPerBlock)
        val maxDataLen = dataBlocks.maxOf { it.size }
        for (i in 0 until maxDataLen) {
            for (b in 0 until numBlocks) {
                if (i < dataBlocks[b].size) result.add(dataBlocks[b][i])
            }
        }
        for (i in 0 until eccPerBlock) {
            for (b in 0 until numBlocks) {
                result.add(eccBlocks[b][i])
            }
        }
        return result.toByteArray()
    }

    private fun computeReedSolomon(data: ByteArray, eccLen: Int): ByteArray {
        val gen = IntArray(eccLen + 1)
        gen[0] = 1
        for (i in 0 until eccLen) {
            val root = GF_EXP[i]
            for (j in i + 1 downTo 1) {
                gen[j] = gen[j] xor gfMul(gen[j - 1], root)
            }
            gen[0] = gfMul(gen[0], 1)
        }

        val rem = IntArray(eccLen)
        for (byte in data) {
            val factor = (byte.toInt() and 0xFF) xor rem[0]
            for (i in 0 until eccLen - 1) rem[i] = rem[i + 1]
            rem[eccLen - 1] = 0
            for (i in 0 until eccLen) {
                rem[i] = rem[i] xor gfMul(gen[i + 1], factor)
            }
        }
        return ByteArray(eccLen) { rem[it].toByte() }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun placeDataBits(
        m: Array<BooleanArray>,
        res: Array<BooleanArray>,
        size: Int,
        codewords: ByteArray
    ) {
        var bitIndex = 0
        val totalBits = codewords.size * 8
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col--
            val rows = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (dc in 0..1) {
                    val c = col - dc
                    if (!res[row][c]) {
                        val bit = if (bitIndex < totalBits) {
                            val byteVal = codewords[bitIndex ushr 3].toInt() and 0xFF
                            ((byteVal ushr (7 - (bitIndex and 7))) and 1) != 0
                        } else false
                        m[row][c] = bit
                        bitIndex++
                    }
                }
            }
            upward = !upward
            col -= 2
        }
    }

    private fun applyMask(m: Array<BooleanArray>, res: Array<BooleanArray>, size: Int, mask: Int) {
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (res[r][c]) continue
                val flip = when (mask) {
                    0 -> (r + c) % 2 == 0
                    1 -> r % 2 == 0
                    2 -> c % 3 == 0
                    3 -> (r + c) % 3 == 0
                    4 -> ((r / 2) + (c / 3)) % 2 == 0
                    5 -> (r * c) % 2 + (r * c) % 3 == 0
                    6 -> ((r * c) % 2 + (r * c) % 3) % 2 == 0
                    else -> ((r + c) % 2 + (r * c) % 3) % 2 == 0
                }
                if (flip) m[r][c] = !m[r][c]
            }
        }
    }

    private fun applyFormatInfo(m: Array<BooleanArray>, size: Int, mask: Int) {
        // Level L (01) + mask (3 bits) -> BCH(15, 5) encoded & XOR 0x5412
        val data = (0b01 shl 3) or mask
        var rem = data shl 10
        for (i in 4 downTo 0) {
            if (((rem ushr (i + 10)) and 1) != 0) {
                rem = rem xor (0b10100110111 shl i)
            }
        }
        val bits = ((data shl 10) or rem) xor 0b101010000010010

        for (i in 0 until 6) m[8][i] = ((bits ushr (14 - i)) and 1) != 0
        m[8][7] = ((bits ushr 8) and 1) != 0
        m[8][8] = ((bits ushr 7) and 1) != 0
        m[7][8] = ((bits ushr 6) and 1) != 0
        for (i in 0 until 6) m[5 - i][8] = ((bits ushr (5 - i)) and 1) != 0

        for (i in 0 until 8) m[8][size - 8 + i] = ((bits ushr (7 - i)) and 1) != 0
        for (i in 0 until 7) m[size - 1 - i][8] = ((bits ushr (14 - i)) and 1) != 0
    }

    private fun scorePenalty(m: Array<BooleanArray>, size: Int): Int {
        var penalty = 0
        for (r in 0 until size) {
            var run = 1
            for (c in 1 until size) {
                if (m[r][c] == m[r][c - 1]) run++ else {
                    if (run >= 5) penalty += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) penalty += 3 + (run - 5)
        }
        for (c in 0 until size) {
            var run = 1
            for (r in 1 until size) {
                if (m[r][c] == m[r - 1][c]) run++ else {
                    if (run >= 5) penalty += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) penalty += 3 + (run - 5)
        }
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val v = m[r][c]
                if (m[r][c + 1] == v && m[r + 1][c] == v && m[r + 1][c + 1] == v) penalty += 3
            }
        }
        return penalty
    }
}
