#import "VidyoPlugin.h"
#import "VidyoViewController.h"

@implementation VidyoPlugin

- (void)pluginInitialize {
    // Register the application default settings from the Settings.bundle to the NSUserDefaults object.
    // Here, the user defaults are loaded only the first time the app is loaded and run.
    
    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    
    NSString *settingsBundle = [[NSBundle mainBundle] pathForResource:@"Settings" ofType:@"bundle"];
    if (!settingsBundle) {
        NSLog(@"Could not find Settings.bundle");
    } else {
        NSDictionary *settings = [NSDictionary dictionaryWithContentsOfFile:[settingsBundle stringByAppendingPathComponent:@"Root.plist"]];
        NSArray *preferences = [settings objectForKey:@"PreferenceSpecifiers"];
        
        for (NSDictionary *prefSpecification in preferences) {
            NSString *key = [prefSpecification objectForKey:@"Key"];
            if (key) {
                // Check if this key was already registered
                if (![standardUserDefaults objectForKey:key]) {
                    [standardUserDefaults setObject:[prefSpecification objectForKey:@"DefaultValue"] forKey:key];
                    
                    NSLog( @"writing as default %@ to the key %@", [prefSpecification objectForKey:@"DefaultValue"], key );
                }
            }
        }
    }
}

- (void)passConnectEvent:(NSString*)event reason: (NSString*)reason  {
    NSDictionary *payload = [NSDictionary dictionaryWithObjectsAndKeys: event, @"event", reason, @"value", nil];
    [self reportEvent: payload];
}

- (void)passDeviceStateEvent:(NSString*)event muted: (NSString*)muted  {
    NSDictionary *payload = [NSDictionary dictionaryWithObjectsAndKeys: event, @"event", muted, @"state", nil];
    [self reportEvent: payload];
}

- (void)passParticipantEvent:(NSString*)event participant: (NSString*)participant  {
    NSDictionary *payload = [NSDictionary dictionaryWithObjectsAndKeys: event, @"event", participant, @"participant", nil];
    [self reportEvent: payload];
}

- (void)reportEvent:(NSDictionary*)payload {
    CDVPluginResult* pluginResult = nil;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:payload];
    [pluginResult setKeepCallbackAsBool:YES];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:pluginCommand.callbackId];
}

- (void)connect:(CDVInvokedUrlCommand *)command {
    /* Store command for further reference */
    pluginCommand = command;
    
    NSString* portal = [command.arguments objectAtIndex:0];
    NSString* roomKey = [command.arguments objectAtIndex:1];
    NSString* displayName = [command.arguments objectAtIndex:2];
    NSString* pin = [command.arguments objectAtIndex:3];
    
    NSNumber* maxParticipants = [command.arguments objectAtIndex:4];
    NSString* logLevel = [command.arguments objectAtIndex:5];

    NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
    
    if (portal != nil) {
        [standardUserDefaults setObject:portal forKey:@"portal"];
    }
    
    if (roomKey != nil) {
        [standardUserDefaults setObject:roomKey forKey:@"roomKey"];
    }
    
    if (displayName != nil) {
        [standardUserDefaults setObject:displayName forKey:@"displayName"];
    }
    
    if (pin != nil) {
        [standardUserDefaults setObject:pin forKey:@"pin"];
    }
    
    [standardUserDefaults setInteger:[maxParticipants intValue] forKey: @"participants"];
    
    if (logLevel != nil) {
        [standardUserDefaults setObject:logLevel forKey:@"logLevel"];
    }
    
    [standardUserDefaults setBool:YES forKey:@"autoJoin"];
    [standardUserDefaults setBool:YES forKey:@"hideConfig"];
    
    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Vidyo" bundle:nil];
    self.vidyoViewController = [storyboard instantiateViewControllerWithIdentifier:@"VidyoViewController"];
        
    if (self.vidyoViewController == nil) {
        self.vidyoViewController = [[VidyoViewController alloc] init];
    }

    /* disable slide donw */
    if (@available(iOS 13.0, *)) {
        self.vidyoViewController.modalInPresentation = TRUE;
    }
    
    self.vidyoViewController.plugin = self;
    
    [self.viewController presentViewController:self.vidyoViewController animated:YES completion:nil];
}

- (void)disconnect:(CDVInvokedUrlCommand *)command {
    if (self.vidyoViewController != nil) {
        [self.vidyoViewController disconnect];
    }
}

- (void)release:(CDVInvokedUrlCommand *)command {
    if (self.vidyoViewController != nil) {
        [self.vidyoViewController close];
    }
}

- (void)setPrivacy:(CDVInvokedUrlCommand *)command {
    NSString* device = [command.arguments objectAtIndex:0];
    BOOL privacy = [command.arguments objectAtIndex:1];
    
    if (self.vidyoViewController != nil) {
        [self.vidyoViewController setPrivacy:device Privacy:privacy];
    }
}

- (void)selectDefaultDevice:(CDVInvokedUrlCommand *)command {
    NSString* device = [command.arguments objectAtIndex:0];

    if (self.vidyoViewController != nil) {
        [self.vidyoViewController selectDefaultDevice:device];
    }
}

- (void)cycleCamera:(CDVInvokedUrlCommand *)command {
    if (self.vidyoViewController != nil) {
        [self.vidyoViewController cycleCamera];
    }
}

- (void)destroy {
    self.vidyoViewController.plugin = nil;
    self.vidyoViewController = nil;
}

@end

