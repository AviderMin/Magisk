package com.topjohnwu.magisk.ui.component

import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.topjohnwu.magisk.core.R
import com.topjohnwu.magisk.core.di.ServiceLocator
import io.github.neoruaa.glasmio.core.GlasmioTheme
import io.github.neoruaa.glasmio.core.IosTypography
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonLayout
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonRole
import io.github.neoruaa.glasmio.ui.GlasmioAlertButtonSpec
import io.github.neoruaa.glasmio.ui.GlasmioAlertDialog
import io.github.neoruaa.glasmio.ui.GlasmioCard
import kotlin.math.abs
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume

sealed interface ConfirmResult {
    data object Confirmed : ConfirmResult
    data object Canceled : ConfirmResult
}

data class DialogVisuals(
    val title: String = "",
    val content: String? = null,
    val markdown: Boolean = false,
    val confirm: String? = null,
    val dismiss: String? = null,
)

private class ConfirmDialogState(
    visible: Boolean = false,
    visuals: DialogVisuals = DialogVisuals(),
) {
    var visible by mutableStateOf(visible)
    var visuals by mutableStateOf(visuals)
    // Whether the dialog was shown by awaitConfirm
    var awaiting = false

    companion object {
        /**
         * Restores the dialog with its content after a configuration change. A dialog shown by
         * awaitConfirm is not restored: the coroutine waiting for its result does not survive,
         * so confirming it would do nothing. Callers that need it show it again.
         */
        val Saver = listSaver<ConfirmDialogState, Any?>(
            save = {
                val v = it.visuals
                listOf(it.visible && !it.awaiting, v.title, v.content, v.markdown, v.confirm, v.dismiss)
            },
            restore = {
                ConfirmDialogState(
                    visible = it[0] as Boolean,
                    visuals = DialogVisuals(
                        title = it[1] as String,
                        content = it[2] as String?,
                        markdown = it[3] as Boolean,
                        confirm = it[4] as String?,
                        dismiss = it[5] as String?,
                    ),
                )
            }
        )
    }
}

interface ConfirmDialogHandle {
    fun showConfirm(
        title: String,
        content: String? = null,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null
    )

    suspend fun awaitConfirm(
        title: String,
        content: String? = null,
        markdown: Boolean = false,
        confirm: String? = null,
        dismiss: String? = null
    ): ConfirmResult
}

private class ConfirmDialogHandleImpl(
    private val state: ConfirmDialogState,
    private val coroutineScope: CoroutineScope,
    private val callback: ConfirmCallback,
    private val resultChannel: Channel<ConfirmResult>
) : ConfirmDialogHandle {

    private var awaitContinuation: CancellableContinuation<ConfirmResult>? = null

    init {
        coroutineScope.launch {
            resultChannel
                .consumeAsFlow()
                .onEach { result ->
                    awaitContinuation?.let {
                        awaitContinuation = null
                        if (it.isActive) it.resume(result)
                    }
                }
                .onEach { state.visible = false }
                .collect { result ->
                    when (result) {
                        ConfirmResult.Confirmed -> callback.onConfirm?.invoke()
                        ConfirmResult.Canceled -> callback.onDismiss?.invoke()
                    }
                }
        }
    }

    override fun showConfirm(
        title: String,
        content: String?,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?
    ) {
        coroutineScope.launch {
            state.visuals = DialogVisuals(title, content, markdown, confirm, dismiss)
            state.awaiting = false
            state.visible = true
        }
    }

    override suspend fun awaitConfirm(
        title: String,
        content: String?,
        markdown: Boolean,
        confirm: String?,
        dismiss: String?
    ): ConfirmResult {
        coroutineScope.launch {
            state.visuals = DialogVisuals(title, content, markdown, confirm, dismiss)
            state.awaiting = true
            state.visible = true
        }
        return suspendCancellableCoroutine { cont ->
            awaitContinuation = cont.apply {
                invokeOnCancellation { state.visible = false }
            }
        }
    }
}

interface ConfirmCallback {
    val onConfirm: (() -> Unit)?
    val onDismiss: (() -> Unit)?
}

@Composable
fun rememberConfirmCallback(
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
): ConfirmCallback {
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    return remember {
        object : ConfirmCallback {
            override val onConfirm get() = currentOnConfirm
            override val onDismiss get() = currentOnDismiss
        }
    }
}

@Composable
fun rememberConfirmDialog(
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
): ConfirmDialogHandle {
    return rememberConfirmDialog(rememberConfirmCallback(onConfirm, onDismiss))
}

@Composable
fun rememberConfirmDialog(callback: ConfirmCallback): ConfirmDialogHandle {
    val state = rememberSaveable(saver = ConfirmDialogState.Saver) { ConfirmDialogState() }
    val scope = rememberCoroutineScope()
    val resultChannel = remember { Channel<ConfirmResult>() }

    val handle = remember {
        ConfirmDialogHandleImpl(state, scope, callback, resultChannel)
    }

    if (state.visible) {
        ConfirmDialogContent(
            visuals = state.visuals,
            confirm = {
                state.visible = false
                scope.launch { resultChannel.send(ConfirmResult.Confirmed) }
            },
            dismiss = {
                state.visible = false
                scope.launch { resultChannel.send(ConfirmResult.Canceled) }
            }
        )
    }

    return handle
}

@Composable
fun LoadingDialog(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (visible) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            GlasmioCard(modifier = modifier.widthIn(max = 320.dp)) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = GlasmioTheme.colors.accent,
                    )
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(R.string.loading),
                        style = IosTypography.body,
                        color = GlasmioTheme.colors.content,
                    )
                }
            }
        }
    }
}

@Composable
fun MagiskDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    title: @Composable (() -> Unit)? = null,
    confirmButton: @Composable (() -> Unit)? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    neutralButton: @Composable (() -> Unit)? = null,
    scrollable: Boolean = false,
    content: @Composable () -> Unit,
) {
    val hasButtons = confirmButton != null || dismissButton != null || neutralButton != null
    GlasmioAlertDialog(
        onDismissRequest = onDismissRequest,
        title = "",
        titleContent = title?.let {
            {
                CompositionLocalProvider(LocalContentColor provides GlasmioTheme.colors.content) {
                    it()
                }
            }
        },
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        DialogBody(scrollable = scrollable, content = content)
        if (hasButtons) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (neutralButton != null) {
                    neutralButton()
                    Spacer(Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (dismissButton != null) {
                    dismissButton()
                }
                if (dismissButton != null && confirmButton != null) {
                    Spacer(Modifier.width(8.dp))
                }
                if (confirmButton != null) {
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun MagiskDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    neutralText: String? = null,
    onNeutral: (() -> Unit)? = null,
    scrollable: Boolean = false,
    content: @Composable () -> Unit,
) {
    val confirmLabel = confirmText ?: stringResource(android.R.string.ok)
    val dismissLabel = dismissText ?: stringResource(android.R.string.cancel)
    val buttons = buildList {
        if (neutralText != null && onNeutral != null) {
            add(
                GlasmioAlertButtonSpec(
                    label = neutralText,
                    onClick = { onNeutral.invoke() },
                    role = GlasmioAlertButtonRole.Default,
                )
            )
        }
        if (dismissText != null || onDismiss != null) {
            add(
                GlasmioAlertButtonSpec(
                    label = dismissLabel,
                    onClick = { onDismiss?.invoke() },
                    role = GlasmioAlertButtonRole.Cancel,
                )
            )
        }
        if (confirmText != null || onConfirm != null) {
            add(
                GlasmioAlertButtonSpec(
                    label = confirmLabel,
                    onClick = { onConfirm?.invoke() },
                    role = GlasmioAlertButtonRole.Default,
                    enabled = confirmEnabled,
                )
            )
        }
    }
    GlasmioAlertDialog(
        onDismissRequest = onDismissRequest,
        title = "",
        titleContent = if (title.isNullOrEmpty()) null else {
            {
                Text(
                    text = title,
                    style = IosTypography.headline,
                    color = GlasmioTheme.colors.content,
                )
            }
        },
        modifier = modifier,
        buttonLayout = if (buttons.size >= 3) {
            GlasmioAlertButtonLayout.Stacked
        } else {
            GlasmioAlertButtonLayout.SideBySide
        },
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
        buttons = buttons,
    ) {
        DialogBody(scrollable = scrollable, content = content)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DialogBody(
    scrollable: Boolean,
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .then(
                if (scrollable) {
                    Modifier
                        .verticalScrollbar(
                            scrollState,
                            contentPadding = PaddingValues(top = 4.dp, bottom = 4.dp, end = 4.dp)
                        )
                        .verticalScroll(scrollState)
                } else Modifier
            )
            .padding(horizontal = 8.dp)
            .padding(bottom = 14.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides GlasmioTheme.colors.content) {
            ProvideTextStyle(IosTypography.body) {
                content()
            }
        }
    }
}

@Composable
fun ConfirmDialogContent(
    visuals: DialogVisuals,
    confirm: () -> Unit,
    dismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    MagiskDialog(
        modifier = modifier,
        onDismissRequest = dismiss,
        title = visuals.title,
        confirmText = visuals.confirm,
        onConfirm = confirm,
        dismissText = visuals.dismiss,
        onDismiss = dismiss,
        scrollable = true,
    ) {
        visuals.content?.let { content ->
            if (visuals.markdown) {
                MarkdownText(content)
            } else {
                Text(
                    text = content,
                    style = IosTypography.body,
                    color = GlasmioTheme.colors.content,
                )
            }
        }
    }
}

private class LinkClickMovementMethod : LinkMovementMethod() {
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val touchSlop = ViewConfiguration.get(widget.context).scaledTouchSlop

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isDragging = false

                val link = findClickableSpan(widget, buffer, event.x, event.y)
                return if (link != null) {
                    Selection.setSelection(buffer, buffer.getSpanStart(link), buffer.getSpanEnd(link))
                    true
                } else {
                    Selection.removeSelection(buffer)
                    false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    if (dx > touchSlop || dy > touchSlop) {
                        isDragging = true
                        Selection.removeSelection(buffer)
                    }
                }
                return false
            }
            MotionEvent.ACTION_UP -> {
                val hasSelection = Selection.getSelectionStart(buffer) != -1
                Selection.removeSelection(buffer)
                if (!isDragging && hasSelection) {
                    val link = findClickableSpan(widget, buffer, downX, downY)
                    if (link != null) {
                        link.onClick(widget)
                        return true
                    }
                }
                return false
            }
            MotionEvent.ACTION_CANCEL -> {
                Selection.removeSelection(buffer)
                isDragging = false
                return false
            }
        }
        return false
    }

    private fun findClickableSpan(widget: TextView, buffer: Spannable, x: Float, y: Float): ClickableSpan? {
        val xInt = (x - widget.totalPaddingLeft + widget.scrollX).toInt()
        val yInt = (y - widget.totalPaddingTop + widget.scrollY).toInt()

        val layout = widget.layout ?: return null
        if (yInt < 0 || yInt >= layout.height) return null

        val line = layout.getLineForVertical(yInt)
        if (xInt < layout.getLineLeft(line) || xInt > layout.getLineRight(line)) {
            return null
        }

        val offset = layout.getOffsetForHorizontal(line, xInt.toFloat())
        val links = buffer.getSpans(offset, offset, ClickableSpan::class.java)
        if (links.isEmpty()) return null

        val link = links[0]
        val start = buffer.getSpanStart(link)
        val end = buffer.getSpanEnd(link)

        val lineStart = layout.getLineStart(line)
        val lineEnd = layout.getLineEnd(line)
        val linkLineStart = maxOf(start, lineStart)
        val linkLineEnd = minOf(end, lineEnd)

        val left = layout.getPrimaryHorizontal(linkLineStart)
        val right = layout.getPrimaryHorizontal(linkLineEnd)
        val minX = minOf(left, right)
        val maxX = maxOf(left, right)

        return if (xInt in minX.toInt()..maxX.toInt()) link else null
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = GlasmioTheme.colors.content,
    linkColor: Color = GlasmioTheme.colors.accent,
) {
    val contentColorArgb = color.toArgb()
    val linkColorArgb = linkColor.toArgb()
    val movementMethod = remember { LinkClickMovementMethod() }
    AndroidView(
        factory = { context ->
            TextView(context).apply {
                setTextColor(contentColorArgb)
                setLinkTextColor(linkColorArgb)
                ServiceLocator.markwon.setMarkdown(this, text)
                this.movementMethod = movementMethod
            }
        },
        update = { textView ->
            textView.setTextColor(contentColorArgb)
            textView.setLinkTextColor(linkColorArgb)
            ServiceLocator.markwon.setMarkdown(textView, text)
            textView.movementMethod = movementMethod
        },
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun MarkdownTextAsync(
    modifier: Modifier = Modifier,
    getMarkdownText: suspend () -> String
) {
    var mdText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            mdText = withContext(Dispatchers.IO) { getMarkdownText() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The text can come from a URL provided by a module, which can be malformed
            Timber.e(e)
            error = true
        }
    }

    when {
        error -> Text(
            text = stringResource(R.string.download_file_error),
            modifier = modifier,
            style = IosTypography.body,
            color = GlasmioTheme.colors.content,
        )
        mdText != null -> MarkdownText(text = mdText!!, modifier = modifier)
        else -> Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GlasmioTheme.colors.accent)
        }
    }
}