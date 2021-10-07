echo Copying unit tests + k8s_ctrl_mock metadata to /tmp
cp -r ../k8s_resources_stest /tmp
runtime=$1
cp $(ls $runtime/bundles/*$runtime*unit*.jar) /tmp