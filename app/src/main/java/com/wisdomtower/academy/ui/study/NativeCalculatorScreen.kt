package com.wisdomtower.academy.ui.study

import androidx.activity.compose.BackHandler
import com.wisdomtower.academy.ui.theme.wisdomTouchFeel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wisdomtower.academy.ui.theme.AppThemeMode
import com.wisdomtower.academy.ui.theme.AtmosphereBackground
import com.wisdomtower.academy.ui.theme.WisdomAccentAmber
import com.wisdomtower.academy.ui.theme.WisdomAccentPurple
import com.wisdomtower.academy.ui.theme.WisdomAccentRose
import com.wisdomtower.academy.ui.theme.WisdomCyan
import com.wisdomtower.academy.ui.theme.WisdomGlassBorder
import com.wisdomtower.academy.ui.theme.WisdomGlassFill
import com.wisdomtower.academy.ui.theme.WisdomGlassFillQuiet
import com.wisdomtower.academy.ui.theme.WisdomGlassFillStrong
import com.wisdomtower.academy.ui.theme.WisdomInk
import com.wisdomtower.academy.ui.theme.WisdomMuted
import java.math.BigDecimal
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Native port of `website-reference/src/components/learning/ScientificCalculator.tsx` (owner problem 1, 2026-10-12):
 * same title ("Scientific Calculator", "Natural Input Mode"), same DEG / RAD toggle (default DEG), same 7 rows of 5 keys
 * in the same order, same key colours, same rules: functions insert "sin(" etc., backspace removes a whole function name,
 * open brackets are closed automatically, "Ans" inserts the last answer, the result is rounded to 10 decimals, an
 * impossible or infinite result shows "Error". The website runs the text through JavaScript; here a small parser does
 * the same job (no code is executed). Not ported: the website's draggable floating window and full-screen toggle (this is
 * a full screen already).
 *
 * D6d (docs/DAY_MODE_PLAN.md, 2026-10-19): day mode, UI part only (the parser is unchanged). Night keeps the exact old
 * literals as `dayNight` pairs. By day: the card and keys are the frosted ivory glass, text is ink, the cyan / purple / amber /
 * rose key texts are the darker day accents, the display is the quiet glass, the "=" key is the deep teal with white text.
 *
 * STATUS: written, NOT compiled, NOT tested on a device.
 */
@Composable
fun NativeCalculatorScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var expression by remember { mutableStateOf("") }
    var displayResult by remember { mutableStateOf("0") }
    var isRad by remember { mutableStateOf(false) }
    var lastAnswer by remember { mutableStateOf("0") }

    val onKey: (CalcKey) -> Unit = { key ->
        when (key.action) {
            CalcAction.Append -> expression += key.token
            CalcAction.Ans -> expression += lastAnswer
            CalcAction.Back -> expression = calcBackspace(expression)
            CalcAction.Clear -> {
                expression = ""
                displayResult = "0"
            }
            CalcAction.Equals -> {
                if (expression.isNotBlank()) {
                    val result = ScientificEvaluator.evaluate(expression, isRad)
                    if (result.isNaN() || result.isInfinite()) {
                        displayResult = "Error"
                    } else {
                        val scaled = result * 1e10
                        val rounded = if (scaled.isInfinite()) result else floor(scaled + 0.5) / 1e10
                        val text = ScientificEvaluator.formatNumber(rounded)
                        displayResult = text
                        lastAnswer = text
                    }
                }
            }
        }
    }

    BackHandler(enabled = true) { onBack() }

    Box(modifier = modifier.fillMaxSize()) {
        AtmosphereBackground(modifier = Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize()) {
            // Shared top toolbar: back arrow + title (48dp touch target, top-left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScreenBackButton(onBack)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CcAccentBg.copy(alpha = 0.20f))
                            .border(BorderStroke(1.dp, CcAccentEdge.copy(alpha = 0.30f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Calculate, contentDescription = null, tint = CcAccent, modifier = Modifier.size(18.dp))
                    }
                    Text("Scientific Calculator", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WisdomInk)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CcCardFill)
                        .border(BorderStroke(1.dp, CcAccentEdge.copy(alpha = 0.30f)), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    // Header inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Natural Input Mode", fontSize = 11.sp, color = CcAccent, fontFamily = FontFamily.Monospace)
                        val modeColor = if (isRad) CcPurpleText else CcAccent
                        val modeBg = if (isRad) CcPurpleBg else CcAccentBg
                        Box(
                            modifier = Modifier.wisdomTouchFeel()
                                .clip(RoundedCornerShape(10.dp))
                                .background(modeBg.copy(alpha = 0.20f))
                                .border(BorderStroke(1.dp, modeBg.copy(alpha = 0.40f)), RoundedCornerShape(10.dp))
                                .clickable { isRad = !isRad }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isRad) "RAD" else "DEG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = modeColor)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(WisdomInk.copy(alpha = 0.10f))
                    )

                    // Display
                    Column(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CcDisplayFill)
                            .border(BorderStroke(1.dp, WisdomInk.copy(alpha = 0.10f)), RoundedCornerShape(16.dp))
                            .heightIn(min = 88.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (expression.isEmpty()) "sin(30) = 0.5" else expression,
                            fontSize = 12.sp,
                            color = CcSlate400,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End,
                            modifier = Modifier.horizontalScroll(rememberScrollState(), reverseScrolling = true)
                        )
                        Text(
                            text = displayResult,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = CcAccent,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .horizontalScroll(rememberScrollState(), reverseScrolling = true)
                        )
                    }

                    // Keypad: 7 rows x 5 keys, same order and colours as the website
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CALC_ROWS.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.forEach { key ->
                                    CalcKeyButton(key = key, onClick = { onKey(key) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---- D6d day / night colours. Night value = the exact old literal; day value = Ivory & Navy token. Getters: read while composing. ----
private fun dayNight(night: Color, day: Color): Color = if (AppThemeMode.isDark) night else day

private val CcAccent: Color get() = dayNight(Color(0xFF67E8F9), WisdomCyan)
private val CcAccentBg: Color get() = dayNight(Color(0xFF06B6D4), WisdomCyan)
private val CcAccentEdge: Color get() = dayNight(Color(0xFF22D3EE), WisdomCyan)
private val CcPurpleText: Color get() = dayNight(Color(0xFFD8B4FE), WisdomAccentPurple)
private val CcPurpleBg: Color get() = dayNight(Color(0xFFA855F7), WisdomAccentPurple)
private val CcSlate400: Color get() = dayNight(Color(0xFF94A3B8), WisdomMuted)
private val CcSlate300: Color get() = dayNight(Color(0xFFCBD5E1), WisdomMuted)
private val CcAmberText: Color get() = dayNight(Color(0xFFFCD34D), WisdomAccentAmber)
private val CcRoseText: Color get() = dayNight(Color(0xFFFDA4AF), WisdomAccentRose)
private val CcCardFill: Color get() = dayNight(Color.White.copy(alpha = 0.04f), WisdomGlassFill)
private val CcDisplayFill: Color get() = dayNight(Color(0x4D040813), WisdomGlassFillQuiet)

private enum class CalcAction { Append, Ans, Back, Clear, Equals }

private enum class CalcStyle { Fn, Const, Paren, Digit, Op, Back, Clear, Ans, Percent, Equals }

private data class CalcKey(
    val label: String,
    val token: String = label,
    val action: CalcAction = CalcAction.Append,
    val style: CalcStyle = CalcStyle.Digit
)

private val CALC_ROWS: List<List<CalcKey>> = listOf(
    listOf(
        CalcKey("sin", "sin(", style = CalcStyle.Fn), CalcKey("cos", "cos(", style = CalcStyle.Fn),
        CalcKey("tan", "tan(", style = CalcStyle.Fn), CalcKey("π", style = CalcStyle.Const), CalcKey("e", style = CalcStyle.Const)
    ),
    listOf(
        CalcKey("asin", "asin(", style = CalcStyle.Fn), CalcKey("acos", "acos(", style = CalcStyle.Fn),
        CalcKey("atan", "atan(", style = CalcStyle.Fn), CalcKey("√", "sqrt(", style = CalcStyle.Const), CalcKey("xʸ", "^", style = CalcStyle.Const)
    ),
    listOf(
        CalcKey("ln", "ln(", style = CalcStyle.Fn), CalcKey("log", "log(", style = CalcStyle.Fn),
        CalcKey("(", style = CalcStyle.Paren), CalcKey(")", style = CalcStyle.Paren), CalcKey("n!", "!", style = CalcStyle.Const)
    ),
    listOf(
        CalcKey("7"), CalcKey("8"), CalcKey("9"),
        CalcKey("⌫", action = CalcAction.Back, style = CalcStyle.Back), CalcKey("AC", action = CalcAction.Clear, style = CalcStyle.Clear)
    ),
    listOf(
        CalcKey("4"), CalcKey("5"), CalcKey("6"), CalcKey("×", style = CalcStyle.Op), CalcKey("÷", style = CalcStyle.Op)
    ),
    listOf(
        CalcKey("1"), CalcKey("2"), CalcKey("3"), CalcKey("+", style = CalcStyle.Op), CalcKey("−", style = CalcStyle.Op)
    ),
    listOf(
        CalcKey("0"), CalcKey("."), CalcKey("Ans", action = CalcAction.Ans, style = CalcStyle.Ans),
        CalcKey("%", style = CalcStyle.Percent), CalcKey("=", action = CalcAction.Equals, style = CalcStyle.Equals)
    )
)

@Composable
private fun CalcKeyButton(key: CalcKey, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var bg = dayNight(Color.White.copy(alpha = 0.05f), WisdomGlassFillStrong)
    var border = BorderStroke(1.dp, dayNight(Color.White.copy(alpha = 0.10f), WisdomGlassBorder))
    var textColor = WisdomInk
    var weight = FontWeight.SemiBold
    when (key.style) {
        CalcStyle.Fn -> textColor = CcAccent
        CalcStyle.Const -> textColor = CcPurpleText
        CalcStyle.Paren -> textColor = CcSlate300
        CalcStyle.Percent -> textColor = CcSlate300
        CalcStyle.Ans -> textColor = CcAmberText
        CalcStyle.Digit -> {
            bg = dayNight(Color.White.copy(alpha = 0.08f), WisdomInk.copy(alpha = 0.07f))
            weight = FontWeight.Bold
        }
        CalcStyle.Op -> {
            bg = CcAccentBg.copy(alpha = 0.20f)
            border = BorderStroke(1.dp, CcAccentEdge.copy(alpha = 0.30f))
            textColor = CcAccent
            weight = FontWeight.Bold
        }
        CalcStyle.Back -> {
            bg = dayNight(Color(0xFFF59E0B), WisdomAccentAmber).copy(alpha = 0.20f)
            border = BorderStroke(1.dp, dayNight(Color(0xFFFBBF24), WisdomAccentAmber).copy(alpha = 0.40f))
            textColor = CcAmberText
        }
        CalcStyle.Clear -> {
            bg = dayNight(Color(0xFFF43F5E), WisdomAccentRose).copy(alpha = 0.20f)
            border = BorderStroke(1.dp, dayNight(Color(0xFFFB7185), WisdomAccentRose).copy(alpha = 0.40f))
            textColor = CcRoseText
            weight = FontWeight.Bold
        }
        CalcStyle.Equals -> {
            bg = dayNight(Color(0xFF22D3EE), WisdomCyan)
            border = BorderStroke(1.dp, dayNight(Color(0xFF22D3EE), WisdomCyan))
            textColor = dayNight(Color(0xFF020617), Color.White)
            weight = FontWeight.Black
        }
    }

    Box(
        modifier = modifier.wisdomTouchFeel()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        if (key.style == CalcStyle.Back) {
            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = textColor, modifier = Modifier.size(18.dp))
        } else {
            Text(
                text = key.label,
                fontSize = if (key.style == CalcStyle.Equals) 17.sp else 14.sp,
                fontWeight = weight,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

/** Backspace of the website: removes a whole function name ("sin(") at the end, otherwise one character. */
private fun calcBackspace(prev: String): String {
    if (prev.isEmpty()) return ""
    val functions = listOf("asin(", "acos(", "atan(", "sqrt(", "sin(", "cos(", "tan(", "log(", "ln(", "abs(")
    for (fn in functions) {
        if (prev.endsWith(fn)) return prev.dropLast(fn.length)
    }
    return prev.dropLast(1)
}

/**
 * Reads the calculator text the way the website's `evaluateScientificExpression` does, without executing code.
 * Grammar: sum of products of powers; `^` is right-associative; `!` and `%` follow a value (`%` divides by 100);
 * functions sin cos tan asin acos atan (degrees unless [isRad]), sqrt, log (base 10), ln, abs; constants π and e.
 * Open brackets are closed at the end. Anything else gives NaN (shown as "Error"), like the website.
 */
internal object ScientificEvaluator {
    private class Fail : Exception()

    fun evaluate(raw: String, isRad: Boolean): Double {
        if (raw.isBlank()) return 0.0
        var depth = 0
        for (ch in raw) {
            if (ch == '(') depth++
            if (ch == ')') depth--
        }
        val sb = StringBuilder(raw)
        while (depth > 0) {
            sb.append(')')
            depth--
        }
        return try {
            Parser(sb.toString(), isRad).parseAll()
        } catch (e: Fail) {
            Double.NaN
        } catch (e: RuntimeException) {
            Double.NaN
        }
    }

    /** Like JavaScript's String(number): "5", "0.5", "1e+21", "1.5e-7". */
    fun formatNumber(d: Double): String {
        if (d == 0.0) return "0"
        if (d.isNaN()) return "NaN"
        if (d.isInfinite()) return if (d > 0) "Infinity" else "-Infinity"
        val a = abs(d)
        if (a >= 1e21 || a < 1e-6) {
            val s = d.toString()
            val idx = s.indexOf('E')
            if (idx < 0) return s
            var mantissa = s.substring(0, idx)
            val exp = s.substring(idx + 1)
            if (mantissa.endsWith(".0")) mantissa = mantissa.dropLast(2)
            val sign = if (exp.startsWith("-")) "" else "+"
            return mantissa + "e" + sign + exp
        }
        return BigDecimal(d.toString()).stripTrailingZeros().toPlainString()
    }

    internal fun factorial(n: Double): Double {
        if (n < 0 || n != floor(n)) return Double.NaN
        if (n > 170) return Double.POSITIVE_INFINITY
        var r = 1.0
        var i = 2
        while (i <= n.toInt()) {
            r *= i
            i++
        }
        return r
    }

    private class Parser(private val s: String, private val isRad: Boolean) {
        private var i = 0

        fun parseAll(): Double {
            val v = parseSum()
            skipSpaces()
            if (i != s.length) throw Fail()
            return v
        }

        private fun peek(): Char = if (i < s.length) s[i] else '\u0000'

        private fun skipSpaces() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun parseSum(): Double {
            var v = parseProduct()
            while (true) {
                skipSpaces()
                val c = peek()
                if (c == '+') {
                    i++
                    v += parseProduct()
                } else if (c == '-' || c == '−') {
                    i++
                    v -= parseProduct()
                } else {
                    return v
                }
            }
        }

        private fun parseProduct(): Double {
            var v = parseUnary()
            while (true) {
                skipSpaces()
                val c = peek()
                if (c == '×' || c == '*') {
                    i++
                    v *= parseUnary()
                } else if (c == '÷' || c == '/') {
                    i++
                    v /= parseUnary()
                } else {
                    return v
                }
            }
        }

        private fun parseUnary(): Double {
            skipSpaces()
            val c = peek()
            if (c == '-' || c == '−') {
                i++
                return -parseUnary()
            }
            if (c == '+') {
                i++
                return parseUnary()
            }
            return parsePower()
        }

        private fun parsePower(): Double {
            val base = parsePostfix()
            skipSpaces()
            if (peek() == '^') {
                i++
                val exponent = parseUnary()
                return Math.pow(base, exponent)
            }
            return base
        }

        private fun parsePostfix(): Double {
            var v = parsePrimary()
            while (true) {
                skipSpaces()
                val c = peek()
                if (c == '!') {
                    i++
                    v = ScientificEvaluator.factorial(v)
                } else if (c == '%') {
                    i++
                    v /= 100.0
                } else {
                    return v
                }
            }
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            val c = peek()
            if (c.isDigit() || c == '.') return parseNumber()
            if (c == '(') {
                i++
                val v = parseSum()
                skipSpaces()
                if (peek() != ')') throw Fail()
                i++
                return v
            }
            if (c == 'π') {
                i++
                return PI
            }
            if (c.isLetter()) {
                val start = i
                while (i < s.length && s[i].isLetter() && s[i] != 'π') i++
                val name = s.substring(start, i)
                if (name == "e") return Math.E
                skipSpaces()
                if (peek() != '(') throw Fail()
                i++
                val arg = parseSum()
                skipSpaces()
                if (peek() != ')') throw Fail()
                i++
                return applyFunction(name, arg)
            }
            throw Fail()
        }

        private fun parseNumber(): Double {
            val start = i
            var dots = 0
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) {
                if (s[i] == '.') dots++
                i++
            }
            if (dots > 1) throw Fail()
            val text = s.substring(start, i)
            if (text == ".") throw Fail()
            return text.toDouble()
        }

        private fun applyFunction(name: String, x: Double): Double {
            val toRad = if (isRad) 1.0 else PI / 180.0
            val fromRad = if (isRad) 1.0 else 180.0 / PI
            return when (name) {
                "sin" -> sin(x * toRad)
                "cos" -> cos(x * toRad)
                "tan" -> tan(x * toRad)
                "asin" -> asin(x) * fromRad
                "acos" -> acos(x) * fromRad
                "atan" -> atan(x) * fromRad
                "sqrt" -> sqrt(x)
                "log" -> log10(x)
                "ln" -> ln(x)
                "abs" -> abs(x)
                else -> throw Fail()
            }
        }
    }
}
