# Playa Photobooth

A fun Android photo booth application designed for Burning Man 2025, featuring selfie segmentation and customizable backgrounds with event branding.

## Features

### >3 Smart Selfie Segmentation
- Uses Google ML Kit for accurate person detection and background replacement
- Real-time camera preview with front-facing camera
- Advanced background replacement with improved mask processing

### <¨ Multiple Background Options
- **Playa Dust**: Default procedural gradient background
- **The Burn**: Custom background from `bg0.png`
- **OMG MUD**: Custom background from `bg1.png`  
- **Poop Today**: Fun custom background from `bg2.png`
- Easy background selection via intuitive thumbnail gallery

### =Ý Text Overlays
- **Event Branding**: "BURNING MAN 2025" header on all photos
- **Custom Messages**: 
  - "Tomorrow, Today." (default backgrounds)
  - "I pooped today!" (special background)
- **Professional Styling**: White text on semi-transparent black backgrounds
- **Responsive Design**: Text scales based on image dimensions

### =ñ Modern UI/UX
- **Material Design 3** theming with Jetpack Compose
- **Background Preview Gallery**: Horizontal scrollable thumbnails at the top
- **Real-time Selection**: Tap any thumbnail to instantly change backgrounds
- **Clean Interface**: Minimal UI that doesn't obstruct camera view

### =÷ Photo Management
- **Gallery Integration**: Photos automatically save to device Photos app
- **Organized Storage**: Creates `Pictures/PlayaPhotobooth/` folder
- **High Quality**: JPEG compression at 90% quality
- **Instant Access**: Photos appear immediately in gallery apps

## Technical Architecture

### Core Components

#### `MainActivity.kt`
- Single Activity architecture
- Navigation between Camera and Gallery screens
- Material Design 3 theme implementation

#### `CameraScreen.kt`
- **Camera Integration**: CameraX for camera preview and capture
- **Segmentation Processing**: ML Kit selfie segmentation with improved float mask handling
- **Background Replacement**: Advanced pixel-by-pixel processing with bilinear interpolation
- **UI Components**: Compose-based camera controls and background selector

#### `BackgroundManager.kt`
- **Dynamic Background Loading**: Automatically loads `bg*.png` files from assets
- **Text Overlay System**: Configurable legend and message text per background
- **Image Processing**: Bitmap manipulation and text rendering
- **Scaling Support**: Responsive background and text sizing

#### `GalleryScreen.kt`
- Photo gallery viewing functionality
- Navigation back to camera

### Technical Details

#### Segmentation Algorithm
- **ML Kit Integration**: Uses `SelfieSegmenterOptions` for single image mode
- **Float Mask Processing**: Handles ML Kit's float confidence values (0.0-1.0)
- **Threshold Detection**: 0.5 confidence threshold for person vs background
- **Coordinate Scaling**: Proper scaling between mask dimensions and image dimensions
- **Error Handling**: Robust fallback to original image on processing failures

#### Background Processing
- **Asset Loading**: Automatic discovery of `bg*.png` files in assets
- **Text Overlay Rendering**: Dynamic text positioning with proper bounds calculation
- **Unified Header Design**: Legend and message in single header block
- **Semi-transparent Backgrounds**: 70% opacity black backgrounds for text readability

#### Photo Storage
- **MediaStore API**: Modern Android 10+ compatible storage
- **Public Gallery**: Photos visible in standard Photos apps
- **Organized Folders**: Dedicated PhotoBooth directory
- **Proper Metadata**: Correct MIME types and file naming

## Build Configuration

### Dependencies
- **Kotlin**: Target JVM 11
- **Android**: Min SDK 24, Target SDK 35, Compile SDK 35
- **Jetpack Compose**: BOM-managed versions
- **CameraX**: 1.3.0 for camera functionality
- **ML Kit**: Selfie segmentation 16.0.0-beta4
- **Material Design 3**: Latest Compose implementation

### Gradle Features
- **Version Catalogs**: Centralized dependency management in `libs.versions.toml`
- **Kotlin Compose Compiler**: Integrated compose compiler plugin
- **ProGuard**: Configured for release builds (currently disabled)

### Build Variants
- **Debug**: Development builds with debugging enabled
- **Release**: Production builds with signing configuration for APK distribution

## Development Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17 or later
- Android SDK with API level 35

### Building
```bash
# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Code quality
./gradlew lint
```

### GitHub Actions CI/CD
- **Automated Building**: Builds debug and release APKs on push/PR
- **Testing**: Runs unit tests and lint checks
- **Artifact Upload**: Stores APKs as GitHub artifacts
- **Release Creation**: Automatic GitHub releases on main branch pushes
- **Versioning**: Uses GitHub run numbers for incremental version codes

## Customization

### Adding New Backgrounds
1. Add `bgN.png` files to `app/src/main/assets/`
2. Update `BackgroundManager.getBackgroundName()` with descriptive names
3. Add text overlay configuration in `backgroundTextOverlays` map
4. Backgrounds are automatically loaded and appear in the selector

### Modifying Text Overlays
Edit the `backgroundTextOverlays` map in `BackgroundManager.kt`:

```kotlin
private val backgroundTextOverlays = mapOf(
    0 to BackgroundTextOverlay("EVENT TITLE", "Custom Message"),
    1 to BackgroundTextOverlay("BURNING MAN 2025", "Tomorrow, Today."),
    // Add more configurations...
)
```

### Styling Text
Modify text appearance in `addTextOverlayToBitmap()`:
- **Font sizes**: `legendTextSize` and `messageTextSize` 
- **Colors**: `legendPaint.color` and `messagePaint.color`
- **Typography**: `Typeface.create()` for font weights
- **Background opacity**: `backgroundPaint.alpha`

## Permissions

The app requires:
- **Camera**: For taking selfie photos
- **Storage**: For saving photos to gallery (handled automatically by MediaStore API)

## License

This project is designed for Burning Man 2025 event use.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes following the existing code style
4. Test thoroughly on physical devices
5. Submit a pull request

## Known Issues

- Segmentation accuracy depends on lighting conditions
- Best results with good contrast between person and background
- Performance may vary on older devices

## Support

For issues or questions, please check the GitHub Issues page or contact the development team.

---

*Built with d for the Burning Man community*