//
//  StatusBar.m
//  网优助手
//
//  Created by 梁仲太 on 2019/2/1.
//

#import "StatusBar.h"


@interface StatusBar()<UIKeyInput>

@property(nonatomic,copy)NSString *callbackId;
@property(nonatomic,copy)NSString *callbackIdKB;
@property(nonatomic,assign)NSInteger statusType;
@property(nonatomic,strong)NSString *color;
@property(nonatomic,assign)CGFloat r;
@property(nonatomic,assign)CGFloat g;
@property(nonatomic,assign)CGFloat b;
@property(nonatomic,assign)CGFloat a;
@property(nonatomic,assign)BOOL defaultStyle;
@property(nonatomic,strong)CDVPluginResult *pluginResult;
@property(nonatomic,assign)BOOL hasAdd;

@end


@implementation StatusBar

-(void)coolMethod:(CDVInvokedUrlCommand *)command{
    NSLog(@"状态栏---cool1");
    self.callbackId = command.callbackId;
    self.statusType = [[command.arguments objectAtIndex:0] integerValue];
    if (self.statusType == STATUS_HEIGHT) {
        //获取状态栏的rect
        CGRect statusRect = [[UIApplication sharedApplication] statusBarFrame];
        //状态栏的高度
        CGFloat h = statusRect.size.height;
        NSLog(@"h=%f",h);
        [self successWithMessage:[NSArray arrayWithObjects:[NSNumber numberWithFloat:h],[NSNumber numberWithFloat:h], nil]];
    } else if (self.statusType == FONT_COLOR_LIGHT) {
        [[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleLightContent];
        return;
    } else if (self.statusType == FONT_COLOR_DART) {
        [[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleDefault];
        return;
    } else if (self.statusType == STATUS_KEYBOARD){
        if (self.hasAdd && self.callbackIdKB != nil) {
            return;
        }
        self.hasAdd = YES;
        self.callbackIdKB = command.callbackId;
        
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                     selector:@selector(keyboardWillShow:)
                                                         name:UIKeyboardWillShowNotification
                                                       object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                    selector:@selector(keyboardWillHide:)
                                                        name:UIKeyboardWillHideNotification
                                                      object:nil];
    } else if(self.statusType == STATUS_FULL_SCREEN_NO) {
        if(command.arguments.count>5){
            self.r = [command.arguments[2] floatValue];
            self.g = [command.arguments[3] floatValue];
            self.b = [command.arguments[4] floatValue];
            self.a = [command.arguments[5] floatValue];
            self.defaultStyle = [command.arguments[6] boolValue];
            [self setStatusBarBackgroundColor:[UIColor colorWithRed:self.r green:self.g blue:self.b alpha:self.a]];
            [[UIApplication sharedApplication] setStatusBarStyle:self.defaultStyle?UIStatusBarStyleDefault:UIStatusBarStyleLightContent];
        }else if(command.arguments.count>1){
            self.color = command.arguments[1];
            self.defaultStyle = [command.arguments[2] boolValue];
            [self setStatusBarBackgroundColor:[StatusBar hexColor:self.color]];
            //[self setStatusBarBackgroundColor:[UIColor colorWithRed:1.0 green:1.0 blue:1.0 alpha:0]];
            //UIStatusBarStyleDefault,黑色(默认)
            //UIStatusBarStyleLightContent,白色
            [[UIApplication sharedApplication] setStatusBarStyle:self.defaultStyle?UIStatusBarStyleDefault:UIStatusBarStyleLightContent];
        }else {
            [self setStatusBarBackgroundColor:[UIColor colorWithRed:1.0 green:1.0 blue:1.0 alpha:0]];
        }
        [self successWithMessage:[NSArray array]];
    } else if(self.statusType == STATUS_FULL_SCREEN){
        //[self setStatusBarBackgroundColor:[UIColor whiteColor]];
        [self setStatusBarBackgroundColor:[UIColor blackColor]];
        if(command.arguments.count>2){
            self.defaultStyle = [command.arguments[2] boolValue];
            [[UIApplication sharedApplication] setStatusBarStyle:self.defaultStyle?UIStatusBarStyleDefault:UIStatusBarStyleLightContent];
        }else{
            [[UIApplication sharedApplication] setStatusBarStyle:UIStatusBarStyleLightContent];
        }
        [self successWithMessage:[NSArray array]];
    } else if(self.statusType == STATUS_HIDE){
        //[self setStatusBarBackgroundColor:[UIColor colorWithWhite:1.0 alpha:0.0]];
        [self successWithMessage:[NSArray array]];
    } else if(self.statusType == STATUS_SHOW){
        //[self setStatusBarBackgroundColor:[UIColor colorWithWhite:1.0 alpha:0.0]];
        [self successWithMessage:[NSArray array]];
    } else if (self.statusType == STATUS_KEYBOARD_DELETE) {
       
    }
}

-(void)deleteBackward {
    
}

+ (UIColor*)hexColor:(NSString*)hexColor {
    unsigned int red, green, blue, alpha;
    NSRange range;
    range.length = 2;
    @try {
        if ([hexColor hasPrefix:@"#"]) {
            hexColor = [hexColor stringByReplacingOccurrencesOfString:@"#" withString:@""];
        }
        range.location = 0;
        [[NSScanner scannerWithString:[hexColor substringWithRange:range]] scanHexInt:&red];
        range.location = 2;
        [[NSScanner scannerWithString:[hexColor substringWithRange:range]] scanHexInt:&green];
        range.location = 4;
        [[NSScanner scannerWithString:[hexColor substringWithRange:range]] scanHexInt:&blue];
        
        if ([hexColor length] > 6) {
            range.location = 6;
            [[NSScanner scannerWithString:[hexColor substringWithRange:range]] scanHexInt:&alpha];
        }else{
            alpha = 255;
        }
    }
    @catch (NSException * e) {
        //        [MAUIToolkit showMessage:[NSString stringWithFormat:@"颜色取值错误:%@,%@", [e name], [e reason]]];
        //        return [UIColor blackColor];
    }
    
    return [UIColor colorWithRed:(float)(red/255.0f) green:(float)(green/255.0f) blue:(float)(blue/255.0f) alpha:(float)(alpha/255.0f)];
}

//设置状态栏背景颜色
- (void)setStatusBarBackgroundColor:(UIColor *)color {
    
    UIView *statusBar = [[[UIApplication sharedApplication] valueForKey:@"statusBarWindow"] valueForKey:@"statusBar"];
    //if ([statusBar respondsToSelector:@selector(setBackgroundColor:)]) {
    statusBar.backgroundColor = color;
    //}
}

- (UIStatusBarStyle)preferredStatusBarStyle{
    return UIStatusBarStyleLightContent;
}

- (void)keyboardWillShow:(NSNotification *)notification {
    NSLog(@"***********键盘打开");
    // 获取键盘的高度
    NSDictionary *userInfo = [notification userInfo];
    NSValue *value = [userInfo objectForKey:UIKeyboardFrameBeginUserInfoKey];
    CGRect keyboardRect = [value CGRectValue];
    int height = keyboardRect.size.height;
    
    
    NSValue *endValue = [userInfo objectForKey:UIKeyboardFrameEndUserInfoKey];
    CGRect keyboardRectEnd = [endValue CGRectValue];
    int heightEnd = keyboardRectEnd.size.height;
    
    
    // NSValue *rightValue = [userInfo objectForKey:@"UIKeyboardBoundsUserInfoKey"];
    // CGRect keyboardRectRight = [rightValue CGRectValue];
    // int heightRight = keyboardRectRight.size.height;
    
    NSLog(@"***********height = %d",height);
    NSLog(@"***********heightEnd = %d",heightEnd);
    // NSLog(@"***********heightRight = %d",heightRight);
    [self successWithMessageKB:[NSArray arrayWithObjects: [NSNumber numberWithBool:true],[NSNumber numberWithInt:heightEnd], nil]];
}
- (void)keyboardWillHide:(NSNotification *)notification {
    NSLog(@"***********键盘关闭");
    // 获取键盘的高度
    NSDictionary *userInfo = [notification userInfo];
    NSValue *value = [userInfo objectForKey:UIKeyboardFrameEndUserInfoKey];
    CGRect keyboardRect = [value CGRectValue];
    int height = keyboardRect.size.height;
    NSLog(@"***********height = %d",height);
    [self successWithMessageKB:[NSArray arrayWithObjects: [NSNumber numberWithBool:false],[NSNumber numberWithInt:0], nil]];
}
- (void)successWithMessage:(NSArray *)messages{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:messages];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
    
}

- (void)successWithMessageKB:(NSArray *)messages {
    if(self.callbackIdKB==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:messages];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackIdKB];
}

- (void)faileWithMessage:(NSString *)message{
    if(self.callbackId==nil)return;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:result callbackId:self.callbackId];
}

@end
