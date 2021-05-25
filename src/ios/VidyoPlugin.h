#import <Cordova/CDVPlugin.h>

@class  VidyoViewController;

@interface VidyoPlugin : CDVPlugin {
@private
    CDVInvokedUrlCommand *pluginCommand;
}

@property (nonatomic, retain) VidyoViewController* vidyoViewController;

- (void)connect:(CDVInvokedUrlCommand *)command;
- (void)disconnect:(CDVInvokedUrlCommand *)command;
- (void)release:(CDVInvokedUrlCommand *)command;

- (void)setPrivacy:(CDVInvokedUrlCommand *)command;
- (void)selectDefaultDevice:(CDVInvokedUrlCommand *)command;
- (void)cycleCamera:(CDVInvokedUrlCommand *)command;

- (void)passConnectEvent:(NSString*)event reason: (NSString*)reason;
- (void)passDeviceStateEvent:(NSString*)event muted: (NSString*)muted;
- (void)passParticipantEvent:(NSString*)event participant: (NSString*)participant;

- (void)destroy;

@end
