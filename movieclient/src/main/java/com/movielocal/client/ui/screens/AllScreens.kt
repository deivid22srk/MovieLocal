package com.movielocal.client.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movielocal.client.data.models.Movie
import com.movielocal.client.data.models.Series
import com.movielocal.client.data.models.Season
import com.movielocal.client.data.models.Episode
import com.movielocal.client.data.models.VideoProgress
import com.movielocal.client.ui.theme.iNoxBlue
import com.movielocal.client.ui.viewmodel.UiState

// ===== HOME SCREEN =====
@Composable
fun HomeScreen(
    uiState: UiState,
    onRefresh: () -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            TopBar(onOpenSettings = onOpenSettings)
        }
        
        item {
            CategoryTabs()
        }
        
        when (uiState) {
            is UiState.Initial -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iNoxBlue)
                    }
                }
            }
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = iNoxBlue)
                    }
                }
            }
            is UiState.Success -> {
                if (uiState.movies.isNotEmpty() || uiState.series.isNotEmpty()) {
                    val featuredContent = uiState.movies.firstOrNull() ?: uiState.series.firstOrNull()
                    
                    if (featuredContent != null) {
                        item {
                            HeroBanner(
                                content = featuredContent,
                                onPlay = {
                                    when (featuredContent) {
                                        is Movie -> onMovieClick(featuredContent)
                                        is Series -> onSeriesClick(featuredContent)
                                    }
                                }
                            )
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(24.dp))
                        ForYouSection()
                    }
                    
                    if (uiState.movies.isNotEmpty()) {
                        item {
                            ContentRow(
                                title = "Movies",
                                items = uiState.movies,
                                onItemClick = { movie ->
                                    onMovieClick(movie)
                                }
                            )
                        }
                    }
                    
                    if (uiState.series.isNotEmpty()) {
                        item {
                            SeriesRow(
                                title = "Series",
                                items = uiState.series,
                                onItemClick = { series ->
                                    onSeriesClick(series)
                                }
                            )
                        }
                    }
                    
                    item {
                        Spacer(Modifier.height(32.dp))
                    }
                } else {
                    item {
                        EmptyState(onRefresh = onRefresh)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    ErrorState(
                        message = uiState.message,
                        onRetry = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "MovieLocal",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Profile",
                tint = iNoxBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CategoryTabs() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Movies", "Series")
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(tabs.size) { index ->
            Surface(
                modifier = Modifier.clickable { selectedTab = index },
                shape = RoundedCornerShape(20.dp),
                color = if (selectedTab == index) iNoxBlue else MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = tabs[index],
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun HeroBanner(
    content: Any,
    onPlay: () -> Unit
) {
    val title = when (content) {
        is Movie -> content.title
        is Series -> content.title
        else -> "Unknown"
    }
    
    val thumbnailUrl = when (content) {
        is Movie -> content.thumbnailUrl
        is Series -> content.thumbnailUrl
        else -> ""
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 200f
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iNoxBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Watch Now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "NOW STREAMING",
                fontSize = 12.sp,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Text(
                text = "Exclusively on MovieLocal",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ForYouSection() {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp)
    ) {
        Text(
            text = "For You",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(listOf("Most Watched", "Recently Add", "Best Rated")) { filter ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (filter == "Most Watched") iNoxBlue else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = filter,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = if (filter == "Most Watched") FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ContentRow(
    title: String,
    items: List<Movie>,
    onItemClick: (Movie) -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { movie ->
                MovieCard(
                    movie = movie,
                    onClick = { onItemClick(movie) }
                )
            }
        }
    }
}

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    isCompleted: Boolean = false
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = movie.thumbnailUrl,
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Assistido",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesRow(
    title: String,
    items: List<Series>,
    onItemClick: (Series) -> Unit
) {
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { series ->
                SeriesCard(
                    series = series,
                    onClick = { onItemClick(series) }
                )
            }
        }
    }
}

@Composable
fun SeriesCard(
    series: Series,
    onClick: () -> Unit,
    hasWatchedEpisodes: Boolean = false
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = series.thumbnailUrl,
                contentDescription = series.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            if (hasWatchedEpisodes) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color(0xFF2196F3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Em Progresso",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Text(
                "No content available",
                fontSize = 18.sp,
                color = Color.Gray
            )
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iNoxBlue
                )
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Red
            )
            Text(
                message,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = iNoxBlue
                )
            ) {
                Text("Retry")
            }
        }
    }
}

// ===== SEARCH SCREEN =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: UiState,
    onSearch: (String) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Most Watched") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Search",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it)
            },
            placeholder = { Text("Find movies, shows, and more", color = Color.Gray) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        
        Spacer(Modifier.height(24.dp))
        
        Column(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = "For You",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listOf("Most Watched", "Recently Add", "Best Rated")) { filter ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (filter == selectedFilter) iNoxBlue else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (filter == selectedFilter) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        when (uiState) {
            is UiState.Initial -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = iNoxBlue)
                }
            }
            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.movies.isNotEmpty()) {
                        item {
                            SearchResultsGrid(
                                title = "Movies",
                                movies = uiState.movies,
                                onMovieClick = { movie ->
                                    onMovieClick(movie)
                                }
                            )
                        }
                    }
                    
                    if (uiState.series.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = "Series",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        items(uiState.series) { series ->
                            SeriesSearchCard(
                                series = series,
                                onClick = { onSeriesClick(series) }
                            )
                        }
                    }
                }
            }
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = iNoxBlue)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = uiState.message,
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsGrid(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies) { movie ->
                Card(
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .clickable { onMovieClick(movie) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    AsyncImage(
                        model = movie.thumbnailUrl,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesSearchCard(
    series: Series,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            AsyncImage(
                model = series.thumbnailUrl,
                contentDescription = series.title,
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = series.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                
                Spacer(Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFB800)
                    )
                    Text(
                        text = series.rating.toString(),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ===== PROFILE SCREEN =====
@Composable
fun ProfileScreen(
    serverUrl: String,
    onOpenSettings: () -> Unit,
    onSwitchProfile: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profileManager = remember { com.movielocal.client.data.ProfileManager(context) }
    val profileName = profileManager.getCurrentProfileName() ?: "MovieLocal User"
    val profileAvatar = profileManager.getCurrentProfileAvatar() ?: "person"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Profile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surface)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getIconForAvatar(profileAvatar),
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = iNoxBlue
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = profileName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(Modifier.height(32.dp))
        
        ProfileOption(
            icon = Icons.Default.SwapHoriz,
            title = "Trocar Perfil",
            subtitle = "Mudar para outro perfil",
            onClick = onSwitchProfile
        )
        
        ProfileOption(
            icon = Icons.Default.Settings,
            title = "Server Settings",
            subtitle = serverUrl.ifEmpty { "Not connected" },
            onClick = onOpenSettings
        )
        
        ProfileOption(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "MovieLocal v1.0",
            onClick = {}
        )
        
        ProfileOption(
            icon = Icons.Default.Download,
            title = "Downloads",
            subtitle = "Manage your downloads",
            onClick = {}
        )
        
        ProfileOption(
            icon = Icons.Default.Notifications,
            title = "Notifications",
            subtitle = "Manage notifications",
            onClick = {}
        )
    }
}

@Composable
fun ProfileOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iNoxBlue,
                modifier = Modifier.size(28.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

// ===== BOTTOM NAVIGATION =====
@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentScreen == "home") Icons.Filled.Home else Icons.Filled.Home,
                    "Home",
                    modifier = Modifier.size(26.dp)
                )
            },
            label = { Text("Home", fontSize = 12.sp) },
            selected = currentScreen == "home",
            onClick = { onNavigate("home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = iNoxBlue,
                selectedTextColor = iNoxBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentScreen == "search") Icons.Filled.Search else Icons.Filled.Search,
                    "Search",
                    modifier = Modifier.size(26.dp)
                )
            },
            label = { Text("Search", fontSize = 12.sp) },
            selected = currentScreen == "search",
            onClick = { onNavigate("search") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = iNoxBlue,
                selectedTextColor = iNoxBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    if (currentScreen == "profile") Icons.Filled.Person else Icons.Filled.Person,
                    "Profile",
                    modifier = Modifier.size(26.dp)
                )
            },
            label = { Text("Profile", fontSize = 12.sp) },
            selected = currentScreen == "profile",
            onClick = { onNavigate("profile") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = iNoxBlue,
                selectedTextColor = iNoxBlue,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

// ===== DIALOGS =====
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFoundDialog(
    serverIp: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.WifiTethering,
                    contentDescription = null,
                    tint = iNoxBlue
                )
                Text("Server Found!")
            }
        },
        text = {
            Text("Found MovieLocal server at:\n$serverIp\n\nWould you like to connect?")
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = iNoxBlue)
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Decline", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDialog(
    currentServerUrl: String,
    isDiscovering: Boolean,
    onSetServerUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onDiscover: () -> Unit
) {
    var serverUrl by remember { mutableStateOf(currentServerUrl) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    tint = iNoxBlue
                )
                Text("Server Settings")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enter server IP address and port:")
                
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("192.168.1.100:8080") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = iNoxBlue,
                        focusedLabelColor = iNoxBlue
                    )
                )
                
                OutlinedButton(
                    onClick = onDiscover,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isDiscovering,
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(iNoxBlue, iNoxBlue))
                    )
                ) {
                    if (isDiscovering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = iNoxBlue
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Searching...", color = iNoxBlue)
                    } else {
                        Icon(Icons.Default.Search, null, tint = iNoxBlue)
                        Spacer(Modifier.width(8.dp))
                        Text("Auto Discover", color = iNoxBlue)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSetServerUrl(serverUrl) },
                enabled = serverUrl.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = iNoxBlue)
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            if (currentServerUrl.isNotEmpty()) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// ===== DETAIL SCREEN =====
@Composable
fun DetailScreen(
    movie: Movie?,
    series: Series?,
    onPlayMovie: (String, String, String) -> Unit,
    onPlaySeries: (Series, Int, Int) -> Unit,
    onBack: () -> Unit
) {
    var isCompleted by remember { mutableStateOf(false) }
    val repository = com.movielocal.client.data.repository.MovieRepository()
    
    LaunchedEffect(movie?.id ?: series?.id) {
        val id = movie?.id ?: series?.id
        if (id != null) {
            try {
                val progress = repository.getProgress(id).getOrNull()
                isCompleted = progress?.completed == true
            } catch (e: Exception) {
            }
        }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                AsyncImage(
                    model = movie?.thumbnailUrl ?: series?.thumbnailUrl,
                    contentDescription = movie?.title ?: series?.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background
                                ),
                                startY = 200f
                            )
                        )
                )
                
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }
        
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = movie?.title ?: series?.title ?: "",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF4CAF50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Assistido",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Assistido",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFFFB800)
                        )
                        Text(
                            text = (movie?.rating ?: series?.rating)?.toString() ?: "0.0",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Text(
                        text = (movie?.year ?: series?.year)?.toString() ?: "",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    Text(
                        text = movie?.genre ?: series?.genre ?: "",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    movie?.duration?.let { duration ->
                        Text(
                            text = "${duration}min",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(Modifier.height(20.dp))
                
                movie?.let {
                    Button(
                        onClick = { onPlayMovie(it.videoUrl, it.id, it.title) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iNoxBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Play Movie",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    text = "Overview",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = movie?.description ?: series?.description ?: "",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    lineHeight = 20.sp
                )
                
                series?.let { seriesData ->
                    Spacer(Modifier.height(32.dp))
                    
                    Text(
                        text = "Episodes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
        
        series?.seasons?.forEach { season ->
            item {
                SeasonSection(
                    series = series,
                    season = season,
                    onEpisodeClick = { episode ->
                        onPlaySeries(series, season.seasonNumber, episode.episodeNumber)
                    }
                )
            }
        }
        
        item {
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SeasonSection(
    series: Series,
    season: Season,
    onEpisodeClick: (Episode) -> Unit
) {
    var expanded by remember { mutableStateOf(season.seasonNumber == 1) }
    val repository = com.movielocal.client.data.repository.MovieRepository()
    var episodeProgress by remember { mutableStateOf<Map<String, VideoProgress>>(emptyMap()) }
    var lastWatchedEpisodeId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(season.seasonNumber) {
        val progressMap = mutableMapOf<String, VideoProgress>()
        var latestTimestamp = 0L
        var latestEpisodeId: String? = null
        
        season.episodes.forEach { episode ->
            try {
                val progress = repository.getProgress(episode.id).getOrNull()
                if (progress != null) {
                    progressMap[episode.id] = progress
                    if (progress.timestamp > latestTimestamp) {
                        latestTimestamp = progress.timestamp
                        latestEpisodeId = episode.id
                    }
                }
            } catch (e: Exception) {
            }
        }
        
        episodeProgress = progressMap
        lastWatchedEpisodeId = latestEpisodeId
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Season ${season.seasonNumber}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White
                )
            }
        }
        
        if (expanded) {
            Spacer(Modifier.height(12.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                season.episodes.forEach { episode ->
                    val progress = episodeProgress[episode.id]
                    val isLastWatched = episode.id == lastWatchedEpisodeId
                    
                    EpisodeCard(
                        episode = episode,
                        onClick = { onEpisodeClick(episode) },
                        isCompleted = progress?.completed == true,
                        isLastWatched = isLastWatched,
                        watchProgress = progress
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun EpisodeCard(
    episode: Episode,
    onClick: () -> Unit,
    isCompleted: Boolean = false,
    isLastWatched: Boolean = false,
    watchProgress: VideoProgress? = null
) {
    val borderColor = when {
        isLastWatched -> Color(0xFFFFB800)
        else -> Color.Transparent
    }
    
    val backgroundColor = when {
        isLastWatched -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = if (isLastWatched) {
            androidx.compose.foundation.BorderStroke(2.dp, borderColor)
        } else null
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box {
                    AsyncImage(
                        model = episode.thumbnailUrl,
                        contentDescription = episode.title,
                        modifier = Modifier
                            .width(120.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Assistido",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    if (watchProgress != null && !isCompleted) {
                        val progressPercent = ((watchProgress.position.toFloat() / watchProgress.duration.toFloat()) * 100).toInt()
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progressPercent / 100f)
                                    .background(iNoxBlue)
                            )
                        }
                    }
                }
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Episode ${episode.episodeNumber}",
                                fontSize = 12.sp,
                                color = if (isLastWatched) Color(0xFFFFB800) else iNoxBlue,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (isCompleted) {
                                Text(
                                    text = "✓",
                                    fontSize = 14.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            if (isLastWatched && !isCompleted) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Continuar",
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Text(
                            text = "${episode.duration}min",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = episode.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        text = episode.description,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            if (isLastWatched && !isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFB800).copy(alpha = 0.1f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Último episódio assistido - Continuar assistindo",
                        fontSize = 11.sp,
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
