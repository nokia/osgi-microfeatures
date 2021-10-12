# Copyright 2000-2021 Nokia
#
# Licensed under the Apache License 2.0
# SPDX-License-Identifier: Apache-2.0
#

echo Copying unit tests + k8s_ctrl_mock metadata to /tmp
cp -r ../k8s_resources_stest /tmp
runtime=$1
cp $(ls $runtime/bundles/*$runtime*unit*.jar) /tmp