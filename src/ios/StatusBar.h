//
//  StatusBar.h
//  网优助手
//
//  Created by 梁仲太 on 2019/2/1.
//

#import <Cordova/CDVPlugin.h>
#import <Cordova/CDV.h>

//黑色
static NSInteger const STATUS_FULL_SCREEN_NO = 0;
//白色
static NSInteger const STATUS_FULL_SCREEN = 1;
//主色
static NSInteger const STATUS_HIDE = 2;
//透明
static NSInteger const STATUS_SHOW = 3;
//获取状态栏高度
static NSInteger const STATUS_HEIGHT = 4;
//监听键盘
static NSInteger const STATUS_KEYBOARD = 5;
// 监听删除
static NSInteger const STATUS_KEYBOARD_DELETE = 6;
// 状态栏字体白色
static NSInteger const FONT_COLOR_LIGHT      = 6;
// 状态栏字体黑色
static NSInteger const FONT_COLOR_DART       = 7;

@interface StatusBar : CDVPlugin

-(void)coolMethod:(CDVInvokedUrlCommand *)command;
-(void)successWithMessage:(NSArray *)messages;
-(void)successWithMessageKB:(NSArray *)messages;
-(void)faileWithMessage:(NSString *)message;

@end
