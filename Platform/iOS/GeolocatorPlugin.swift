import CoreLocation
import Foundation

@MainActor final class GeolocatorPlugin: NSObject, @preconcurrency CLLocationManagerDelegate {
    private static var instance: GeolocatorPlugin?
    private let manager = CLLocationManager()
    private var pendingReply: PluginReply?
    private var highAccuracy = false
    static func register() {
        if instance == nil {
            instance = GeolocatorPlugin()
        }
    }

    private override init() {
        super.init()
        manager.delegate = self
        let channel = NativeChannels.channel("dotnative.geolocator")
        channel.onReset = {
            [weak self] in self?.cancel()
        }
        channel.handle("getCurrentPosition") {
            [weak self] args, reply in
            self?.request(args, reply) ?? reply.failure("unavailable", "Geolocator is unavailable")
        }
    }

    private func request(_ arguments: PluginValue, _ reply: PluginReply) {
        guard pendingReply == nil else {
            reply.failure("busy", "A position request is already pending")
            return
        }
        let authorization = manager.authorizationStatus
        guard authorization == .authorizedAlways || authorization == .authorizedWhenInUse else {
            reply.failure(
                "permission_denied",
                "Grant when-in-use location permission before reading a position")
            return
        }
        if case .bool(let value)? = arguments.fields["highAccuracy"] {
            highAccuracy = value
        } else {
            highAccuracy = false
        }
        manager.desiredAccuracy =
            highAccuracy ? kCLLocationAccuracyBest : kCLLocationAccuracyHundredMeters
        pendingReply = reply
        reply.onCancel = {
            [weak self] in
            Task {
                @MainActor in self?.cancel()
            }
        }
        manager.requestLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last, let reply = pendingReply else {
            return
        }
        pendingReply = nil
        reply.success(
            .map([
                "latitude": .double(location.coordinate.latitude),
                "longitude": .double(location.coordinate.longitude),
                "accuracy": .double(location.horizontalAccuracy),
                "altitude": .double(location.altitude),
                "timestamp": .integer(Int64(location.timestamp.timeIntervalSince1970 * 1000)),
            ]))
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        guard let reply = pendingReply else {
            return
        }
        pendingReply = nil
        reply.failure("location_failed", error.localizedDescription)
    }

    private func cancel() {
        manager.stopUpdatingLocation()
        pendingReply = nil
    }
}
