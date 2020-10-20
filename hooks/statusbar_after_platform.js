//create by 梁仲太 2018-6-28
module.exports = function (ctx) {
    //将相应代码写入activity
    replaceGradleFile(ctx);

    function replaceGradleFile(ctx) {
        const Q = ctx.requireCordovaModule('q');
        const path = ctx.requireCordovaModule('path');
        const fs = ctx.requireCordovaModule('fs');
        const pRoot = ctx.opts.projectRoot;

        const packageJsonPath = path.resolve(__dirname, '../package.json');
        const packageJson = require(packageJsonPath);
        const packageName = 'com/chinamobile/gdwy';
        const appGradle = path.join(pRoot, 'platforms/android/app/build.gradle');
        const mainActivity = path.join(pRoot, 'platforms/android/app/src/main/java/'+packageName+'/MainActivity.java');
        const manifestXml = path.join(pRoot, 'platforms/android/app/src/main/AndroidManifest.xml');

        /*const color = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values/colors.xml');
        const styles = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values/styles.xml');

        const colorV19 = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values-v19/colors.xml');
        const stylesV19 = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values-v19/styles.xml');
        const colorV21 = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values-v21/colors.xml');
        const stylesV21 = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values-v21/styles.xml');
        const colorV23 = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values-v23/colors.xml');
        const stylesV23 = path.join(pRoot,'plugins/com/chinamobile/status/statusbar/src/android/values-v23/styles.xml');


        const colorTarget = path.join(pRoot,'platforms/android/app/src/main/res/values/colors.xml');
        const stylesTarget = path.join(pRoot,'platforms/android/app/src/main/res/values/styles.xml');

        const colorV19Target = path.join(pRoot,'platforms/android/app/src/main/res/values-v19/colors.xml');
        const stylesV19Target = path.join(pRoot,'platforms/android/app/src/main/res/values-v19/styles.xml');
        const colorV21Target = path.join(pRoot,'platforms/android/app/src/main/res/values-v21/colors.xml');
        const stylesV21Target = path.join(pRoot,'platforms/android/app/src/main/res/values-v21/styles.xml');
        const colorV23Target = path.join(pRoot,'platforms/android/app/src/main/res/values-v23/colors.xml');
        const stylesV23Target = path.join(pRoot,'platforms/android/app/src/main/res/values-v23/styles.xml');*/
        console.log("--------------statusbar修改源码开始");
        //如果是android平台
        if (fs.existsSync(appGradle)) {
            const data = fs.readFileSync(manifestXml, 'utf8');
            if (data.indexOf("@style/BaseTheme") == -1) {
                console.log("--------------修改MainActivity");
                //修改mainActivity
                replace_string_in_file(fs,mainActivity,
                'super.onCreate(savedInstanceState);',
                'super.onCreate(savedInstanceState);com.chinamobile.status.StatusBar.initStatusBar(this);');
                //修改manifest
                replace_string_in_file(fs,manifestXml,
                '<application ',
                '<application android:theme=\"@style/BaseTheme\" ');
                console.log("--------------修改MainActivity成功");
            }

            //修改values目录
            // write_file(fs,color,colorTarget);
            // write_file(fs,styles,stylesTarget);
            // console.log("--------------修改values目录成功");

            //创建values-v19目录
            /*fs.mkdirSync("./platforms/android/app/src/main/res/values-v19",function(err){
                if (err) {
                   return console.error("--------------创建values-v19异常"+err);
                }
                console.log("--------------目录values-v19创建成功。");
                write_file(fs,colorV19,colorV19Target);
                write_file(fs,stylesV19,stylesV19Target);
                console.log("--------------目录values-v19写入成功。");

                //创建values-v21目录
                fs.mkdirSync("./platforms/android/app/src/main/res/values-v21",function(err){
                    if (err) {
                       return console.error("创建values-v21异常"+err);
                    }
                    console.log("--------------目录values-v21创建成功。");
                    write_file(fs,colorV21,colorV21Target);
                    write_file(fs,stylesV21,stylesV21Target);
                    console.log("--------------目录values-v21写入成功。");

                    //创建values-v23目录
                    fs.mkdirSync("./platforms/android/app/src/main/res/values-v23",function(err){
                       if (err) {
                           return console.error("创建values-v23异常"+err);
                       }
                        console.log("--------------目录values-v23创建成功。");
                        write_file(fs,colorV23,colorV23Target);
                        write_file(fs,stylesV23,stylesV23Target);
                        console.log("--------------目录values-v23写入成功。");
                    });
                });
            });*/
        }

    }

    //替换文件中的指定内容
    function replace_string_in_file(fs, filename, to_replace, replace_with) {
        const data = fs.readFileSync(filename, 'utf8');
        const result = data.replace(to_replace, replace_with);
        fs.writeFileSync(filename, result, 'utf8');
    }
    //写入文件
    function write_file(fs, source, target) {
        var readable = fs.createReadStream(source);
        var writable = fs.createWriteStream(target);
        readable.pipe(writable);
    }
}