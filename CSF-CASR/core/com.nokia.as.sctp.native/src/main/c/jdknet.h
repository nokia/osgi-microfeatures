#include <jni.h>
#ifndef JDKNET_H_
#define JDKNET_H_

jobject sockAddrToInetSocketAddress(JNIEnv* env, struct sockaddr_storage* sap);
struct sockaddr_storage InetSocketAddressToSockAddr(JNIEnv *env, jobject iaObj, jboolean v4mappedAddress);


#endif /* JDKNET_H_ */
