using DotNative.Plugins;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;

namespace DotNative.Geolocator;

public sealed record Position(
    double Latitude,
    double Longitude,
    double AccuracyMeters,
    double AltitudeMeters,
    DateTimeOffset Timestamp
);

public interface IGeolocator
{
    Task<Position> GetCurrentPositionAsync(
        bool highAccuracy = false,
        CancellationToken cancellationToken = default
    );
}

public static class GeolocatorServices
{
    public static IServiceCollection AddGeolocator(this IServiceCollection services)
    {
        services.TryAddSingleton<IGeolocator, ChannelGeolocator>();
        return services;
    }
}

internal sealed class ChannelGeolocator(IPlatformChannels channels) : IGeolocator
{
    public async Task<Position> GetCurrentPositionAsync(
        bool highAccuracy = false,
        CancellationToken cancellationToken = default
    )
    {
        var result = await channels
            .Get("dotnative.geolocator")
            .InvokeAsync(
                "getCurrentPosition",
                new Dictionary<string, object?> { ["highAccuracy"] = highAccuracy },
                cancellationToken
            )
            .ConfigureAwait(false);
        if (
            result is not Dictionary<string, object?> map
            || map.GetValueOrDefault("latitude") is not double latitude
            || map.GetValueOrDefault("longitude") is not double longitude
            || map.GetValueOrDefault("accuracy") is not double accuracy
            || map.GetValueOrDefault("altitude") is not double altitude
            || map.GetValueOrDefault("timestamp") is not long timestamp
        )
            throw new InvalidDataException("Invalid native position response.");
        return new(
            latitude,
            longitude,
            accuracy,
            altitude,
            DateTimeOffset.FromUnixTimeMilliseconds(timestamp)
        );
    }
}
