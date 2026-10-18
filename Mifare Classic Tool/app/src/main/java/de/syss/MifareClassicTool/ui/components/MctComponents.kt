package de.syss.MifareClassicTool.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.syss.MifareClassicTool.R

// =========================================================================
// KernelSU Style UI 2.0 Component Library
// =========================================================================

/**
 * Modern KernelSU style TopAppBar with clean title, back navigation icon, and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MctTopAppBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("top_bar_back")
                ) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

/**
 * Section title header for setting groups and content sections.
 */
@Composable
fun MctSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

/**
 * KernelSU Style Grouped Card Container.
 * Encapsulates setting items or details in an elevated rounded card with razor-sharp border.
 */
@Composable
fun MctSettingGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrEmpty()) {
            MctSectionHeader(title = title)
            Spacer(modifier = Modifier.height(4.dp))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

/**
 * Setting Item row with leading icon, title, description, and trailing value/chevron.
 */
@Composable
fun MctSettingItem(
    title: String,
    description: String? = null,
    icon: Painter? = null,
    iconTint: Color? = null,
    valueText: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    testTag: String = "",
    trailingContent: (@Composable () -> Unit)? = null
) {
    val clickableModifier = if (onClick != null && enabled) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(iconTint ?: MaterialTheme.colorScheme.primary)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        } else if (valueText != null || onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (valueText != null) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Setting Item row with right-aligned Switch.
 */
@Composable
fun MctSwitchItem(
    title: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: Painter? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
    testTag: String = ""
) {
    MctSettingItem(
        title = title,
        description = description,
        icon = icon,
        iconTint = iconTint,
        enabled = enabled,
        onClick = if (enabled) { { onCheckedChange(!checked) } } else null,
        testTag = testTag,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    )
}

/**
 * Radio Selection Item for choices inside dialogs or grouped containers.
 */
@Composable
fun MctSelectionItem(
    title: String,
    description: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * KernelSU Style Status Hero Card.
 * Prominently presents core system / hardware status with state badge, title, subtitle, and action chip.
 */
@Composable
fun MctStatusCard(
    title: String,
    subtitle: String,
    isReady: Boolean,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    iconPainter: Painter? = null
) {
    val statusColor = when {
        isReady -> MaterialTheme.colorScheme.secondary
        isWarning -> MaterialTheme.colorScheme.error.copy(red = 0.9f, green = 0.6f, blue = 0.1f)
        else -> MaterialTheme.colorScheme.error
    }
    val containerColor = when {
        isReady -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        isWarning -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconPainter != null) {
                        Image(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(statusColor)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (actionText != null && onActionClick != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Universal Section Card (upgraded to KernelSU UI 2.0 aesthetics with backward compatibility).
 */
@Composable
fun MctSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Primary Action Button with 12dp rounded corners and tech blue styling.
 */
@Composable
fun MctActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    testTag: String = ""
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(testTag)
    ) {
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/**
 * Danger / Destructive Action Button for high-risk operations (write card, clear logs).
 */
@Composable
fun MctDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconPainter: Painter? = null,
    testTag: String = ""
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(testTag)
    ) {
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onError)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

/**
 * Monospace Field for technical data (UID, Hex, Block addresses).
 */
@Composable
fun MctMonospaceField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    placeholder: String = "",
    actionPainter: Painter? = null,
    actionDescription: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
    testTag: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .testTag(testTag),
            readOnly = readOnly,
            placeholder = { Text(text = placeholder) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
        if (actionPainter != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = onActionClick,
                enabled = actionEnabled,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(52.dp)
                    .testTag("${testTag}_action"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Image(
                    painter = actionPainter,
                    contentDescription = actionDescription,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Notice / Safety Warning Card with rounded corners and subtle tinted border.
 */
@Composable
fun MctInfoCard(
    text: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false,
    icon: Painter? = null
) {
    val borderColor = if (isWarning) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }
    val containerColor = if (isWarning) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Modern 24dp Rounded Selection Dialog for vertical choice selection (UID Format, Language, Theme).
 */
@Composable
fun MctSelectionDialog(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEachIndexed { index, option ->
                    MctSelectionItem(
                        title = option,
                        selected = index == selectedIndex,
                        onClick = {
                            onSelect(index)
                            onDismissRequest()
                        },
                        testTag = "dialog_option_$index"
                    )
                    if (index < options.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

/**
 * Modern Confirmation Dialog.
 */
@Composable
fun MctConfirmDialog(
    title: String,
    message: String,
    confirmText: String = stringResource(R.string.action_ok),
    dismissText: String = stringResource(R.string.action_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDanger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (isDanger) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/**
 * Placeholder for empty lists / states.
 */
@Composable
fun MctEmptyState(
    message: String,
    iconPainter: Painter? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
