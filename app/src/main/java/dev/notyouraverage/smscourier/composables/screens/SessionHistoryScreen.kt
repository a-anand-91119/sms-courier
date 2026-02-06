package dev.notyouraverage.smscourier.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.notyouraverage.smscourier.composables.components.ContactCard
import dev.notyouraverage.smscourier.composables.components.MessageDetailBottomSheet
import dev.notyouraverage.smscourier.composables.components.SessionCard
import dev.notyouraverage.smscourier.composables.components.SkeletonSessionCard
import dev.notyouraverage.smscourier.data.entities.ForwardingSession
import dev.notyouraverage.smscourier.repository.ForwardedMessageRepository
import dev.notyouraverage.smscourier.viewmodels.SessionHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    viewModel: SessionHistoryViewModel,
    messageRepository: ForwardedMessageRepository,
    onNavigateBack: () -> Unit,
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedSession by viewModel.selectedSession.collectAsState()
    val sessions = viewModel.sessions.collectAsLazyPagingItems()
    val contacts = viewModel.contacts.collectAsLazyPagingItems()
    val contactMessageCounts by viewModel.contactMessageCounts.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Messages for selected session (collected only when session is selected)
    val messagesFlow = remember(selectedSession) {
        selectedSession?.let { session ->
            messageRepository.getMessagesForSessionPaged(session.id)
        }
    }
    val messages = messagesFlow?.collectAsLazyPagingItems()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "Session History",
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )

                // Tab row - per CONTEXT.md: "Top bar tabs below screen title (underlined active tab style)"
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    Tab(
                        selected = selectedTab == SessionHistoryViewModel.Tab.SESSIONS,
                        onClick = { viewModel.selectTab(SessionHistoryViewModel.Tab.SESSIONS) },
                        text = { Text("Sessions") },
                    )
                    Tab(
                        selected = selectedTab == SessionHistoryViewModel.Tab.CONTACTS,
                        onClick = { viewModel.selectTab(SessionHistoryViewModel.Tab.CONTACTS) },
                        text = { Text("Contacts") },
                    )
                }
            }
        },
    ) { paddingValues ->
        when (selectedTab) {
            SessionHistoryViewModel.Tab.SESSIONS -> {
                SessionsList(
                    sessions = sessions,
                    onSessionClick = { viewModel.selectSession(it) },
                    modifier = Modifier.padding(paddingValues),
                )
            }
            SessionHistoryViewModel.Tab.CONTACTS -> {
                ContactsList(
                    contacts = contacts,
                    contactMessageCounts = contactMessageCounts,
                    onLoadMessageCount = { viewModel.loadContactMessageCount(it) },
                    onContactClick = { /* TODO: Phase 18 could filter by contact */ },
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }

    // Message detail bottom sheet - shown when a session is selected
    selectedSession?.let { session ->
        if (messages != null) {
            MessageDetailBottomSheet(
                session = session,
                messages = messages,
                onDismiss = { viewModel.selectSession(null) },
            )
        }
    }
}

@Composable
private fun SessionsList(
    sessions: LazyPagingItems<ForwardingSession>,
    onSessionClick: (ForwardingSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (sessions.loadState.refresh) {
        is LoadState.Loading -> {
            // Initial loading state with skeleton cards
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(5) {
                    SkeletonSessionCard()
                }
            }
        }
        is LoadState.Error -> {
            // Error state
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Failed to load sessions. Pull down to retry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        is LoadState.NotLoading -> {
            if (sessions.itemCount == 0) {
                // Empty state
                EmptySessionsState(modifier = modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        count = sessions.itemCount,
                        key = sessions.itemKey { it.id },
                    ) { index ->
                        val session = sessions[index]
                        if (session != null) {
                            SessionCard(
                                session = session,
                                onClick = { onSessionClick(session) },
                            )
                        } else {
                            SkeletonSessionCard()
                        }
                    }

                    // Bottom padding for scroll
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsList(
    contacts: LazyPagingItems<String>,
    contactMessageCounts: Map<String, Int>,
    onLoadMessageCount: (String) -> Unit,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (contacts.loadState.refresh) {
        is LoadState.Loading -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(5) {
                    SkeletonSessionCard()
                }
            }
        }
        is LoadState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Failed to load contacts. Pull down to retry.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        is LoadState.NotLoading -> {
            if (contacts.itemCount == 0) {
                EmptyContactsState(modifier = modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        count = contacts.itemCount,
                        key = contacts.itemKey { it },
                    ) { index ->
                        val senderNumber = contacts[index]
                        if (senderNumber != null) {
                            // Load message count on first render
                            LaunchedEffect(senderNumber) {
                                onLoadMessageCount(senderNumber)
                            }
                            ContactCard(
                                senderNumber = senderNumber,
                                messageCount = contactMessageCounts[senderNumber],
                                onClick = { onContactClick(senderNumber) },
                            )
                        } else {
                            SkeletonSessionCard()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySessionsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "No sessions yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Forwarding sessions with this device will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmptyContactsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "No contacts yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Senders from forwarded messages will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
