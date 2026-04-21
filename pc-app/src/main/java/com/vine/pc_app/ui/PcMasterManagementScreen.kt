package com.vine.pc_app.ui
import com.vine.pc_app.data.PcDependencies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vine.inventory_contract.DeleteMasterRecordCommand
import com.vine.inventory_contract.GetMasterRecordsQuery
import com.vine.inventory_contract.MasterRecordDetail
import com.vine.inventory_contract.MasterRecordSummary
import com.vine.inventory_contract.MasterType
import com.vine.inventory_contract.SaveMasterRecordCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class MasterEditorState(
    val code: String = "",
    val name: String = "",
    val warehouseCode: String = "",
    val parentCode: String = "",
    val sortOrder: String = "0",
    val isActive: Boolean = true,
    val note: String = "",
)

@Composable
fun PcMasterManagementScreen() {
    var selectedType by remember { mutableStateOf<MasterType?>(null) }

    if (selectedType == null) {
        PcMasterTypeListScreen(
            onSelectType = { selectedType = it },
        )
    } else {
        PcMasterDetailScreen(
            type = selectedType!!,
            onBack = { selectedType = null },
        )
    }
}

@Composable
private fun PcMasterTypeListScreen(
    onSelectType: (MasterType) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "マスタ一覧",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "管理したいマスタを選択してください。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        MasterType.entries.forEach { type ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectType(type) },
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = masterDescription(type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = "開く →",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun PcMasterDetailScreen(
    type: MasterType,
    onBack: () -> Unit,
) {
    val repository = remember { PcDependencies.masterRepository }
    val scope = rememberCoroutineScope()

    var keyword by remember(type) { mutableStateOf("") }
    var includeInactive by remember(type) { mutableStateOf(false) }

    var summaries by remember(type) { mutableStateOf(emptyList<MasterRecordSummary>()) }
    var selectedCode by remember(type) { mutableStateOf<String?>(null) }
    var editor by remember(type) { mutableStateOf(MasterEditorState()) }

    var loading by remember(type) { mutableStateOf(false) }
    var showDeleteDialog by remember(type) { mutableStateOf(false) }

    var infoMessage by remember(type) { mutableStateOf<String?>(null) }
    var errorMessage by remember(type) { mutableStateOf<String?>(null) }

    var reloadKey by remember(type) { mutableIntStateOf(0) }

    suspend fun reloadList(keepSelectedCode: String? = selectedCode) {
        loading = true
        errorMessage = null

        try {
            val rows = withContext(Dispatchers.IO) {
                repository.getSummaries(
                    GetMasterRecordsQuery(
                        type = type,
                        keyword = keyword,
                        includeInactive = includeInactive,
                        limit = 500,
                    )
                )
            }

            summaries = rows

            if (keepSelectedCode != null && rows.any { it.code == keepSelectedCode }) {
                val detail = withContext(Dispatchers.IO) {
                    repository.getDetail(type, keepSelectedCode)
                }
                if (detail != null) {
                    selectedCode = detail.code
                    editor = detail.toEditorState()
                }
            } else {
                selectedCode = null
                editor = MasterEditorState()
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "一覧取得に失敗しました。"
        } finally {
            loading = false
        }
    }

    suspend fun loadDetail(code: String) {
        loading = true
        errorMessage = null
        infoMessage = null

        try {
            val detail = withContext(Dispatchers.IO) {
                repository.getDetail(type, code)
            }

            if (detail == null) {
                errorMessage = "対象データが見つかりません。"
                return
            }

            selectedCode = detail.code
            editor = detail.toEditorState()
        } catch (e: Exception) {
            errorMessage = e.message ?: "詳細取得に失敗しました。"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(type, reloadKey) {
        reloadList(selectedCode)
    }

    if (showDeleteDialog && selectedCode != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("削除確認") },
            text = {
                Text("${type.label}「${selectedCode}」を無効化します。よろしいですか？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val code = selectedCode ?: return@TextButton
                        showDeleteDialog = false

                        scope.launch {
                            loading = true
                            errorMessage = null
                            infoMessage = null

                            try {
                                withContext(Dispatchers.IO) {
                                    repository.delete(
                                        DeleteMasterRecordCommand(
                                            type = type,
                                            code = code,
                                        )
                                    )
                                }

                                infoMessage = "無効化しました。"
                                selectedCode = null
                                editor = MasterEditorState()
                                reloadKey++
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "無効化に失敗しました。"
                            } finally {
                                loading = false
                            }
                        }
                    }
                ) {
                    Text("無効化する")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("キャンセル")
                }
            },
        )
    }

    val activeCount = summaries.count { it.isActive }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("← マスタ一覧へ戻る")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${type.label}マスタ詳細",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.weight(1f))

            if (loading) {
                Text(
                    text = "読込中...",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (infoMessage != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = infoMessage!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (errorMessage != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "現在一覧",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "表示件数: ${summaries.size}件 / 有効: ${activeCount}件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("検索（コード / 名称）") },
                        singleLine = true,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = includeInactive,
                            onCheckedChange = {
                                includeInactive = it
                                reloadKey++
                            },
                        )
                        Text("無効データも表示")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { reloadKey++ },
                        ) {
                            Text("検索")
                        }

                        TextButton(
                            onClick = {
                                selectedCode = null
                                editor = MasterEditorState()
                                infoMessage = null
                                errorMessage = null
                            },
                        ) {
                            Text("新規")
                        }
                    }

                    HorizontalDivider()

                    if (summaries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "表示できるデータがありません。",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(summaries) { row ->
                                val selected = row.code == selectedCode

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { loadDetail(row.code) }
                                        },
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                }
                                            )
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Text(
                                            text = row.code,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )

                                        Text(
                                            text = row.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                        )

                                        if (type == MasterType.LOCATION) {
                                            Text(
                                                text = "倉庫: ${row.warehouseCode.orEmpty()} / 親: ${row.parentCode.orEmpty()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        Text(
                                            text = if (row.isActive) "有効" else "無効",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (row.isActive) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = if (selectedCode == null) {
                            "${type.label} 新規登録"
                        } else {
                            "${type.label} 編集"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    OutlinedTextField(
                        value = editor.code,
                        onValueChange = { editor = editor.copy(code = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("コード") },
                        singleLine = true,
                        enabled = selectedCode == null,
                    )

                    if (selectedCode != null) {
                        Text(
                            text = "既存データのコードは変更不可です。変更したい場合は新規で登録してください。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = { editor = editor.copy(name = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("名称") },
                        singleLine = true,
                    )

                    if (type == MasterType.LOCATION) {
                        OutlinedTextField(
                            value = editor.warehouseCode,
                            onValueChange = { editor = editor.copy(warehouseCode = it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("倉庫コード") },
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = editor.parentCode,
                            onValueChange = { editor = editor.copy(parentCode = it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("親ロケーションコード") },
                            singleLine = true,
                        )
                    }

                    OutlinedTextField(
                        value = editor.sortOrder,
                        onValueChange = { value ->
                            editor = editor.copy(
                                sortOrder = value.filter { it.isDigit() || it == '-' }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("表示順") },
                        singleLine = true,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = editor.isActive,
                            onCheckedChange = { checked ->
                                editor = editor.copy(isActive = checked)
                            },
                        )
                        Text("有効")
                    }

                    OutlinedTextField(
                        value = editor.note,
                        onValueChange = { editor = editor.copy(note = it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        label = { Text("備考") },
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    errorMessage = null
                                    infoMessage = null

                                    try {
                                        if (type == MasterType.LOCATION && editor.warehouseCode.isBlank()) {
                                            throw IllegalArgumentException("ロケーションは倉庫コードが必須です。")
                                        }

                                        val isNew = selectedCode == null

                                        val saved = withContext(Dispatchers.IO) {
                                            repository.save(editor.toCommand(type))
                                        }

                                        selectedCode = saved.code
                                        editor = saved.toEditorState()
                                        infoMessage = if (isNew) "登録しました。" else "更新しました。"
                                        reloadKey++
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "保存に失敗しました。"
                                    } finally {
                                        loading = false
                                    }
                                }
                            },
                        ) {
                            Text(if (selectedCode == null) "登録" else "保存")
                        }

                        TextButton(
                            onClick = {
                                if (selectedCode == null) {
                                    editor = MasterEditorState()
                                } else {
                                    scope.launch { loadDetail(selectedCode!!) }
                                }
                            },
                        ) {
                            Text("元に戻す")
                        }

                        TextButton(
                            onClick = {
                                if (selectedCode != null) {
                                    showDeleteDialog = true
                                }
                            },
                            enabled = selectedCode != null,
                        ) {
                            Text("削除")
                        }
                    }
                }
            }
        }
    }
}

private fun MasterRecordDetail.toEditorState(): MasterEditorState =
    MasterEditorState(
        code = code,
        name = name,
        warehouseCode = warehouseCode.orEmpty(),
        parentCode = parentCode.orEmpty(),
        sortOrder = sortOrder.toString(),
        isActive = isActive,
        note = note.orEmpty(),
    )

private fun MasterEditorState.toCommand(
    type: MasterType,
): SaveMasterRecordCommand =
    SaveMasterRecordCommand(
        type = type,
        code = code,
        name = name,
        warehouseCode = warehouseCode.ifBlank { null },
        parentCode = parentCode.ifBlank { null },
        sortOrder = sortOrder.toIntOrNull() ?: 0,
        isActive = isActive,
        note = note.ifBlank { null },
    )

private fun masterDescription(type: MasterType): String =
    when (type) {
        MasterType.PRODUCT -> "商品コード・商品名を管理します。"
        MasterType.WAREHOUSE -> "倉庫コード・倉庫名を管理します。"
        MasterType.LOCATION -> "ロケーションと所属倉庫を管理します。"
        MasterType.OPERATOR -> "作業担当者を管理します。"
        MasterType.REASON -> "調整理由・操作理由を管理します。"
    }