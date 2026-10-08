using System;
using Microsoft.Extensions.DependencyInjection;

namespace DotNative.Geolocator;

public static class GeolocatorServiceProviderExtensions
{
#if NET10_0_OR_GREATER
    extension(IServiceProvider services)
    {
        /// <summary>Resolves the registered plugin using the provider's DI lifetime.</summary>
        public IGeolocator Geolocator => services.GetRequiredService<IGeolocator>();
    }
#else
    /// <summary>Resolves the registered plugin using the provider's DI lifetime.</summary>
    public static IGeolocator Geolocator(this IServiceProvider services) =>
        services.GetRequiredService<IGeolocator>();
#endif
}
