# DotNative.Geolocator

Reads the current foreground location from Android or iOS.

```csharp
builder.Services.AddGeolocator();
var position = await services.Geolocator
    .GetCurrentPositionAsync(highAccuracy: true, cancellationToken);
```

`Position` contains latitude, longitude, horizontal accuracy, altitude, and the
location timestamp. Call `IPermissions.RequestAsync(PermissionKind.Location)`
from `DotNative.Permissions` before requesting a position. A request returns a
`PluginException` if permission is missing or the platform has no location fix.
Cancellation stops the pending native request.

| Platform | Status |
| --- | --- |
| Android | GPS/network location providers; fine or coarse runtime permission |
| iOS | Core Location one-shot request; when-in-use authorization |
| macOS | Not implemented |
| Windows | Not implemented |
| Linux | Not implemented |

Declare Android coarse/fine location in the manifest. iOS needs
`NSLocationWhenInUseUsageDescription`. This port implements one-shot lookup only;
continuous position streams, geofences, background location and platform-specific
settings are not included. This independent DotNative implementation is MIT licensed.

## Service access

Import `DotNative.Geolocator` to access the plugin through `IServiceProvider`:

```csharp
using DotNative.Geolocator;

var plugin = services.Geolocator;
```

The getter calls `GetRequiredService<IGeolocator>()` on every access, preserving
DI lifetimes and the usual missing-registration error. Register the plugin with
`AddGeolocator(...)` before building the provider.

A `net10.0` application uses the property syntax with C# 14 or later. A
`net9.0` application uses only the method equivalent:

```csharp
var plugin = services.Geolocator();
```

The package contains separate `net9.0` and `net10.0` assemblies. NuGet selects
the assembly matching the application target framework. `NET10_0_OR_GREATER`
selects the property; the `#else` branch selects the method.

Build and pack both targets with .NET 10 SDK. A source build using .NET 9 SDK
builds only `net9.0`; it does not produce the .NET 10 assembly.
