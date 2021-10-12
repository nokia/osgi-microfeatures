// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package $basePackageName$;

import org.osgi.service.component.annotations.*;

@Component
public class Example {

    @Activate
    void start() {
        System.out.println("Example.start");
    }

}
