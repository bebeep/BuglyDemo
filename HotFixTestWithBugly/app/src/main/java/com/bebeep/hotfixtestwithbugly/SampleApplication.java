package com.bebeep.hotfixtestwithbugly;

import com.tencent.tinker.loader.app.TinkerApplication;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created by Administrator on 2017/3/21.
 */

public class SampleApplication extends TinkerApplication {
    public SampleApplication() {
        super(ShareConstants.TINKER_ENABLE_ALL, "com.bebeep.hotfixtestwithbugly.SampleApplicationLike",
                "com.tencent.tinker.loader.TinkerLoader", false);
    }
}
