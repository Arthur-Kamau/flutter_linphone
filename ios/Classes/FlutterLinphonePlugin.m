#import "FlutterLinphonePlugin.h"
#if __has_include(<flutter_linphone/flutter_linphone-Swift.h>)
#import <flutter_linphone/flutter_linphone-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_linphone-Swift.h"
#endif

@implementation FlutterLinphonePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterLinphonePlugin registerWithRegistrar:registrar];
}
@end
