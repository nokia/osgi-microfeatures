#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/sctp.h>
#include <arpa/inet.h>
#include "Sctp.h"
#include "jdknet.h"

typedef enum {
	SCTP_CID_DATA			= 0,
    SCTP_CID_INIT			= 1,
    SCTP_CID_INIT_ACK		= 2,
    SCTP_CID_SACK			= 3,
    SCTP_CID_HEARTBEAT		= 4,
    SCTP_CID_HEARTBEAT_ACK	= 5,
    SCTP_CID_ABORT			= 6,
    SCTP_CID_SHUTDOWN		= 7,
    SCTP_CID_SHUTDOWN_ACK	= 8,
    SCTP_CID_ERROR			= 9,
    SCTP_CID_COOKIE_ECHO	= 10,
    SCTP_CID_COOKIE_ACK	    = 11,
    SCTP_CID_ECN_ECNE		= 12,
    SCTP_CID_ECN_CWR		= 13,
    SCTP_CID_SHUTDOWN_COMPLETE	= 14,
	SCTP_CID_AUTH			= 0x0F,
	SCTP_CID_FWD_TSN		= 0xC0,
	SCTP_CID_ASCONF			= 0xC1,
	SCTP_CID_ASCONF_ACK		= 0x80,
	SCTP_CID_RECONF			= 0x82,
} sctp_cid_t;

static char* JAVA_SCTP_RTOINFO = "com/alcatel/as/util/sctp/sctp_rtoinfo";
static char* JAVA_SCTP_ASSOCPARAMS = "com/alcatel/as/util/sctp/sctp_assocparams";
static char* JAVA_SCTP_INITMSG = "com/alcatel/as/util/sctp/sctp_initmsg";
static char* JAVA_SCTP_SETADAPTATION = "com/alcatel/as/util/sctp/sctp_setadaptation";
static char* JAVA_SCTP_PADDRPARAMS = "com/alcatel/as/util/sctp/sctp_paddrparams";
static char* JAVA_SCTP_PADDRPARAMS_INIT = "(ILjava/net/InetSocketAddress;JIJJLcom/alcatel/as/util/sctp/sctp_spp_flags;)V";
static char* JAVA_SCTP_SPPFLAGS = "com/alcatel/as/util/sctp/sctp_spp_flags";
static char* JAVA_SCTP_SPPFLAGS_JNI = "Lcom/alcatel/as/util/sctp/sctp_spp_flags;";
static char* JAVA_SCTP_SNDRCVINFO = "com/alcatel/as/util/sctp/sctp_sndrcvinfo";
static char* JAVA_SCTP_SNDRCVINFO_INIT = "(IIILcom/alcatel/as/util/sctp/sctp_sinfo_flags;JJJJJ)V";
static char* JAVA_SCTP_SINFOFLAGS = "com/alcatel/as/util/sctp/sctp_sinfo_flags";
static char* JAVA_SCTP_SINFOFLAGS_JNI = "Lcom/alcatel/as/util/sctp/sctp_sinfo_flags;";
static char* JAVA_SCTP_EVENTSUBSCRIBE = "com/alcatel/as/util/sctp/sctp_event_subscribe";
static char* JAVA_SCTP_ASSOCVALUE = "com/alcatel/as/util/sctp/sctp_assoc_value";
static char* JAVA_SCTP_STATUS = "com/alcatel/as/util/sctp/sctp_status";
static char* JAVA_SCTP_STATUS_INIT = "(ILcom/alcatel/as/util/sctp/sctp_status$sctp_sstat_state;JIIIIJLcom/alcatel/as/util/sctp/sctp_paddrinfo;)V";
static char* JAVA_SCTP_SSTATSTATE = "com/alcatel/as/util/sctp/sctp_status$sctp_sstat_state";
static char* JAVA_SCTP_PADDRINFO = "com/alcatel/as/util/sctp/sctp_paddrinfo";
static char* JAVA_SCTP_PADDRINFO_INIT = "(ILjava/net/InetSocketAddress;Lcom/alcatel/as/util/sctp/sctp_paddrinfo$sctp_spinfo_state;JJJJ)V";
static char* JAVA_SCTP_SPINFOSTATE = "com/alcatel/as/util/sctp/sctp_paddrinfo$sctp_spinfo_state";
static char* JAVA_SCTP_SACKINFO = "com/alcatel/as/util/sctp/sctp_sack_info";
static char* JAVA_SCTP_CIDT = "com/alcatel/as/util/sctp/sctp_authchunk$sctp_cid_t";
static char* JAVA_SCTP_CIDT_JNI = "Lcom/alcatel/as/util/sctp/sctp_authchunk$sctp_cid_t;";
static char* JAVA_SCTP_HMACALGO = "com/alcatel/as/util/sctp/sctp_hmacalgo";
static char* JAVA_SCTP_HMACALGO_INIT = "(J[Lcom/alcatel/as/util/sctp/sctp_hmacalgo$idents;)V";
static char* JAVA_SCTP_HMACALGOIDENTS = "com/alcatel/as/util/sctp/sctp_hmacalgo$idents";
static char* JAVA_SCTP_HMACALGOIDENTS_JNI = "[Lcom/alcatel/as/util/sctp/sctp_hmacalgo$idents;";
static char* JAVA_SCTP_AUTHKEYID = "com/alcatel/as/util/sctp/sctp_authkeyid";
static char* JAVA_SCTP_AUTHCHUNKS = "com/alcatel/as/util/sctp/sctp_authchunks";
static char* JAVA_SCTP_AUTHCHUNKS_INIT = "(IJ[Lcom/alcatel/as/util/sctp/sctp_authchunk$sctp_cid_t;)V";

static void checkException(JNIEnv* env, int line) {
	if((*env) -> ExceptionCheck(env)) {
		(*env) -> ExceptionDescribe(env);
		fprintf(stderr, "Java exception in Sctp.c line %d\n", line);
		abort();
	}
}

jboolean int2jboolean(int i) {
	return i ? JNI_TRUE : JNI_FALSE;
}

int jboolean2int(jboolean b) {
	if(b == JNI_TRUE) return 1;
	return 0;
}

jint throwIOException(JNIEnv* env, char* format, ...) __attribute__ ((format (printf, 2, 3)));
jint throwIOException(JNIEnv* env, char* format, ...) {
	jclass exClass = (*env) -> FindClass(env, "java/io/IOException");
	checkException(env, __LINE__);

	char* message;
	va_list args;

	va_start(args, format);
	if(0 > vasprintf(&message, format, args)) message = NULL;
	va_end(args);

	jint res;
    if(message) {
		res = (*env) -> ThrowNew(env, exClass, message);
		free(message);
	} else {
		res = (*env) -> ThrowNew(env, exClass, "Unknown error");
	}
	return res;
}

jint getIntField(JNIEnv* env, jobject obj, jclass class, char* name) {
	jfieldID field = (*env) -> GetFieldID(env, class, name, "I");
	checkException(env, __LINE__);
	return (*env) -> GetIntField(env, obj, field);
}

jobject getArrayField(JNIEnv* env, jobject obj, jclass class, char* className, char* name) {

	jfieldID field = (*env) -> GetFieldID(env, class, name, className);
	checkException(env, __LINE__);
	jobject mvdata = (*env) -> GetObjectField(env, obj, field);
	checkException(env, __LINE__);
	return mvdata;
}

jlong getLongField(JNIEnv* env, jobject obj, jclass class, char* name) {
	jfieldID field = (*env) -> GetFieldID(env, class, name, "J");
	checkException(env, __LINE__);
	return (*env) -> GetLongField(env, obj, field);
}

int getBooleanField(JNIEnv* env, jobject obj, jclass class, char* name) {
	jfieldID field = (*env) -> GetFieldID(env, class, name, "Z");
	checkException(env, __LINE__);
	return jboolean2int((*env) -> GetBooleanField(env, obj, field));
}

jobject getEnum(JNIEnv* env, char* class, char* val) {

	int len = strlen(class) + 3;
	char buf[len];
	jclass enumClass = (*env) -> FindClass(env, class);
	checkException(env, __LINE__);
	snprintf(buf, sizeof(buf), "%s%s%s", "L", class, ";");
	jfieldID enumFID = (*env) -> GetStaticFieldID(env, enumClass, val, buf);
	checkException(env, __LINE__);
	jobject jenum = (*env) -> GetStaticObjectField(env, enumClass, enumFID);
	checkException(env, __LINE__);
	return jenum;
}

char* getEnumValue(JNIEnv* env, jobject object, char* enumClassName) {
	jclass enumClass = (*env) -> FindClass(env, enumClassName);
	checkException(env, __LINE__);
	jmethodID getNameMethod = (*env) -> GetMethodID(env, enumClass, "name", "()Ljava/lang/String;");
	checkException(env, __LINE__);
	jstring value = (jstring) (*env) -> CallObjectMethod(env, object, getNameMethod);
	checkException(env, __LINE__);
	return (char*) (*env) -> GetStringUTFChars(env, value, 0);
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1RTOINFO(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_rtoinfo rtoinfo;
	rtoinfo.srto_assoc_id = associd;
	memset((void*) &rtoinfo, 0, sizeof(rtoinfo));
	socklen_t sz = sizeof(rtoinfo);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_RTOINFO, &rtoinfo, &sz)) {
		throwIOException(env, "Get SCTP_RTOINFO failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_RTOINFO);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(IJJJ)V");
	checkException(env, __LINE__);

	jobject jrtoinfo = (*env) -> NewObject(env, cls, midInit, rtoinfo.srto_assoc_id, 
															  (unsigned long) rtoinfo.srto_initial, 
															  (unsigned long) rtoinfo.srto_max, 
															  (unsigned long) rtoinfo.srto_min);
	return jrtoinfo;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1RTOINFO(JNIEnv* env, jclass this, jint fd, jobject jrtoinfo) {
	jclass jrtoinfoClass = (*env) -> GetObjectClass(env, jrtoinfo);
	checkException(env, __LINE__);

	struct sctp_rtoinfo rtoinfo;
	memset((void*) &rtoinfo, 0, sizeof(rtoinfo));
	rtoinfo.srto_assoc_id = getIntField(env, jrtoinfo, jrtoinfoClass, "sctp_assoc_id");
	rtoinfo.srto_initial = getLongField(env, jrtoinfo, jrtoinfoClass, "srto_initial");
	rtoinfo.srto_max = getLongField(env, jrtoinfo, jrtoinfoClass, "srto_max");
	rtoinfo.srto_min = getLongField(env, jrtoinfo, jrtoinfoClass, "srto_min");

	checkException(env, __LINE__);

	socklen_t sz = sizeof(rtoinfo);
	if(setsockopt(fd, IPPROTO_SCTP, SCTP_RTOINFO, &rtoinfo, sz)) {
		throwIOException(env, "Set SCTP_RTOINFO failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1ASSOCINFO(JNIEnv* env, jclass this, jint fd, jint assocID) {
	struct sctp_assocparams associnfo;
	memset((void*) &associnfo, 0, sizeof(associnfo));
	associnfo.sasoc_assoc_id = assocID;
	socklen_t sz = sizeof(associnfo);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_ASSOCINFO, &associnfo, &sz)) {
		throwIOException(env, "Get SCTP_ASSOCINFO failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_ASSOCPARAMS);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(IIIJJJ)V");
	checkException(env, __LINE__);

	jobject jassocinfo = (*env) -> NewObject(env, cls, midInit, associnfo.sasoc_assoc_id, 
																associnfo.sasoc_asocmaxrxt, 
																associnfo.sasoc_number_peer_destinations,
																(unsigned long) associnfo.sasoc_peer_rwnd, 
																(unsigned long) associnfo.sasoc_local_rwnd, 
																(unsigned long) associnfo.sasoc_cookie_life);
	return jassocinfo;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1ASSOCINFO(JNIEnv* env, jclass this, jint fd, jobject jassocinfo) {
	jclass jassocinfoClass = (*env) -> GetObjectClass(env, jassocinfo);
	checkException(env, __LINE__);

	struct sctp_assocparams associnfo;
	memset((void*) &associnfo, 0, sizeof(associnfo));
	associnfo.sasoc_assoc_id = getIntField(env, jassocinfo, jassocinfoClass, "sasoc_assoc_id");
	associnfo.sasoc_asocmaxrxt = getIntField(env, jassocinfo, jassocinfoClass, "sasoc_asocmaxrxt");
	associnfo.sasoc_cookie_life = getLongField(env, jassocinfo, jassocinfoClass, "sasoc_cookie_life");

	checkException(env, __LINE__);

	socklen_t sz = sizeof(associnfo);
	if(setsockopt(fd, IPPROTO_SCTP, SCTP_ASSOCINFO, &associnfo, sz)) {
		throwIOException(env, "Set SCTP_ASSOCINFO failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1INITMSG(JNIEnv* env, jclass this, jint fd) {
	struct sctp_initmsg initmsg;
	memset((void*) &initmsg, 0, sizeof(initmsg));
	socklen_t sz = sizeof(initmsg);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_INITMSG, &initmsg, &sz)) {
		throwIOException(env, "Get SCTP_INITMSG failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_INITMSG);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(IIII)V");
	checkException(env, __LINE__);

	jobject jinitmsg = (*env) -> NewObject(env, cls, midInit, initmsg.sinit_num_ostreams, initmsg.sinit_max_instreams,
															  initmsg.sinit_max_attempts, initmsg.sinit_max_init_timeo);
	return jinitmsg;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1INITMSG(JNIEnv* env, jclass this, jint fd, jobject jinitmsg) {
	jclass jinitmsgClass = (*env) -> GetObjectClass(env, jinitmsg);
	checkException(env, __LINE__);

	struct sctp_initmsg initmsg;
	memset((void*) &initmsg, 0, sizeof(initmsg));
	initmsg.sinit_num_ostreams = getIntField(env, jinitmsg, jinitmsgClass, "sinit_num_ostreams");
	initmsg.sinit_max_instreams = getIntField(env, jinitmsg, jinitmsgClass, "sinit_max_instreams");
	initmsg.sinit_max_attempts = getIntField(env, jinitmsg, jinitmsgClass, "sinit_max_attempts");
	initmsg.sinit_max_init_timeo = getIntField(env, jinitmsg, jinitmsgClass, "sinit_max_init_timeo");

	checkException(env, __LINE__);

	socklen_t sz = sizeof(initmsg);
	if(setsockopt(fd, IPPROTO_SCTP, SCTP_INITMSG, &initmsg, sz)) {
		throwIOException(env, "Set SCTP_INITMSG failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jlong JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1AUTOCLOSE(JNIEnv* env, jclass this, jint fd) {
	int autoclose;
	socklen_t sz = sizeof(autoclose);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_AUTOCLOSE, &autoclose, &sz)) {
		throwIOException(env, "Get SCTP_AUTOCLOSE failed: %s", strerror(errno));
		return -1;
	}
	checkException(env, __LINE__);

	return autoclose;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1AUTOCLOSE(JNIEnv* env, jclass this, jint fd, jlong autoclose) {
	socklen_t sz = sizeof(autoclose);
	if(setsockopt(fd, IPPROTO_SCTP, SCTP_AUTOCLOSE, &autoclose, sz)) {
		throwIOException(env, "Set SCTP_AUTOCLOSE failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1ADAPTATION_1LAYER(JNIEnv* env, jclass this, jint fd) {
	struct sctp_setadaptation adaptation;
	memset((void*) &adaptation, 0, sizeof(adaptation));
	socklen_t sz = sizeof(adaptation);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_ADAPTATION_LAYER, &adaptation, &sz)) {
		throwIOException(env, "Get SCTP_ADAPTATION_LAYER failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_SETADAPTATION);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(J)V");
	checkException(env, __LINE__);

	jobject jadaptation = (*env) -> NewObject(env, cls, midInit, (unsigned long) adaptation.ssb_adaptation_ind);
	return jadaptation;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1ADAPTATION_1LAYER(JNIEnv* env, jclass this, jint fd, jobject jadaptation) {
	jclass jadaptationClass = (*env) -> GetObjectClass(env, jadaptation);
	checkException(env, __LINE__);

	jlong ssb_adaptation_ind = getLongField(env, jadaptation, jadaptationClass, "ssb_adaptation_ind");
	checkException(env, __LINE__);

	struct sctp_setadaptation adaptation;
	memset((void*) &adaptation, 0, sizeof(adaptation));
	adaptation.ssb_adaptation_ind = ssb_adaptation_ind;

	socklen_t sz = sizeof(adaptation);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_ADAPTATION_LAYER, &adaptation, sz)) {
		throwIOException(env, "Get SCTP_ADAPTATION_LAYER failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1PEER_1ADDR_1PARAMS(JNIEnv* env, jclass this, jint fd, jint assocID) {
	struct sctp_paddrparams paddr;
	memset((void*) &paddr, 0, sizeof(paddr));
	paddr.spp_address.ss_family = AF_INET;
	paddr.spp_assoc_id = assocID;

	socklen_t sz = sizeof(struct sctp_paddrparams);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_PEER_ADDR_PARAMS, &paddr, &sz)) {
		throwIOException(env, "Get SCTP_PEER_ADDR_PARAMS failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_PADDRPARAMS);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", JAVA_SCTP_PADDRPARAMS_INIT);
	checkException(env, __LINE__);

	jobject address = sockAddrToInetSocketAddress(env, &paddr.spp_address);

	jclass flagscls = (*env) -> FindClass(env, JAVA_SCTP_SPPFLAGS);
	checkException(env, __LINE__);
	jmethodID flagsInit = (*env) -> GetMethodID(env, flagscls, "<init>", "(J)V");
	checkException(env, __LINE__);
	jobject flags = (*env) -> NewObject(env, flagscls, flagsInit, paddr.spp_flags);
	jobject jpaddr = (*env) -> NewObject(env, cls, midInit, paddr.spp_assoc_id, 
															address, 
															(unsigned long) paddr.spp_hbinterval, 
															paddr.spp_pathmaxrxt,
															(unsigned long) paddr.spp_pathmtu, 
															(unsigned long) paddr.spp_sackdelay, 
															flags);
	return jpaddr;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1PEER_1ADDR_1PARAMS(JNIEnv* env, jclass this, jint fd, jobject jpaddr) {
	jclass jpaddrClass = (*env) -> GetObjectClass(env, jpaddr);
	checkException(env, __LINE__);

	jfieldID inetaddrID = (*env) -> GetFieldID(env, jpaddrClass, "spp_address", "Ljava/net/InetSocketAddress;");
	checkException(env, __LINE__);
	jobject inetaddr = (*env)->GetObjectField(env, jpaddr, inetaddrID);
	checkException(env, __LINE__);

	struct sockaddr_storage storage = InetSocketAddressToSockAddr(env, inetaddr, Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1I_1WANT_1MAPPED_1V4_1ADDR(env, this, fd));

	jint spp_assoc_id = getIntField(env, jpaddr, jpaddrClass, "spp_assoc_id");
	jlong spp_hbinterval = getLongField(env, jpaddr, jpaddrClass, "spp_hbinterval");
	jint spp_pathmaxrxt = getIntField(env, jpaddr, jpaddrClass, "spp_pathmaxrxt");
	jlong spp_pathmtu = getLongField(env, jpaddr, jpaddrClass, "spp_pathmtu");
	jlong spp_sackdelay = getLongField(env, jpaddr, jpaddrClass, "spp_sackdelay");
	checkException(env, __LINE__);

	jclass f = (*env)->FindClass(env, JAVA_SCTP_SPPFLAGS);
	checkException(env, __LINE__);
	jfieldID flagsID = (*env) -> GetFieldID(env, jpaddrClass, "spp_flags", JAVA_SCTP_SPPFLAGS_JNI);
	checkException(env, __LINE__);
	jobject flags = (*env)->GetObjectField(env, jpaddr, flagsID);
	jlong flag = getLongField(env, flags, f, "flags");
	checkException(env, __LINE__);

	struct sctp_paddrparams paddr;
	memset((void*) &paddr, 0, sizeof(paddr));
//	paddr.spp_address = storage;
	paddr.spp_address.ss_family = AF_INET;
	paddr.spp_hbinterval = spp_hbinterval;
	paddr.spp_pathmaxrxt = spp_pathmaxrxt;
	paddr.spp_pathmtu = spp_pathmtu;
	paddr.spp_sackdelay = spp_sackdelay;
	paddr.spp_flags = flag;

	socklen_t sz = sizeof(paddr);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_PEER_ADDR_PARAMS, &paddr, sz)) {
		throwIOException(env, "Set SCTP_PEER_ADDR_PARAMS failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1DEFAULT_1SEND_1PARAM(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_sndrcvinfo info;
	memset((void*) &info, 0, sizeof(info));
	info.sinfo_assoc_id = associd;

	socklen_t sz = sizeof(struct sctp_sndrcvinfo);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_DEFAULT_SEND_PARAM, &info, &sz)) {
		throwIOException(env, "Get SCTP_DEFAULT_SEND_PARAM failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_SNDRCVINFO);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", JAVA_SCTP_SNDRCVINFO_INIT);
	checkException(env, __LINE__);

	jclass flagscls = (*env) -> FindClass(env, JAVA_SCTP_SINFOFLAGS);
	checkException(env, __LINE__);
	jmethodID flagsInit = (*env) -> GetMethodID(env, flagscls, "<init>", "(J)V");
	checkException(env, __LINE__);
	jobject flags = (*env) -> NewObject(env, flagscls, flagsInit, info.sinfo_flags);
	jobject jinfo = (*env) -> NewObject(env, cls, midInit, info.sinfo_assoc_id, 
														   info.sinfo_stream, 
														   info.sinfo_ssn, 
														   flags,
														   (unsigned long) info.sinfo_ppid, 
														   (unsigned long) info.sinfo_context, 
														   (unsigned long) info.sinfo_timetolive,
														   (unsigned long) info.sinfo_tsn, 
														   (unsigned long) info.sinfo_cumtsn);
	return jinfo;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1DEFAULT_1SEND_1PARAM(JNIEnv* env, jclass this, jint fd, jobject jinfo) {
	jclass jinfoClass = (*env) -> GetObjectClass(env, jinfo);
	checkException(env, __LINE__);

	jint sinfo_assoc_id = getIntField(env, jinfo, jinfoClass, "sinfo_assoc_id");
	jint sinfo_stream = getIntField(env, jinfo, jinfoClass, "sinfo_stream");
	jint sinfo_ssn = getIntField(env, jinfo, jinfoClass, "sinfo_ssn");
	jlong sinfo_ppid = getLongField(env, jinfo, jinfoClass, "sinfo_ppid");
	jlong sinfo_context = getLongField(env, jinfo, jinfoClass, "sinfo_context");
	jlong sinfo_timetolive = getLongField(env, jinfo, jinfoClass, "sinfo_timetolive");
	jlong sinfo_tsn = getLongField(env, jinfo, jinfoClass, "sinfo_tsn");
	jlong sinfo_cumtsn = getLongField(env, jinfo, jinfoClass, "sinfo_cumtsn");
	checkException(env, __LINE__);

	jclass f = (*env)->FindClass(env, JAVA_SCTP_SINFOFLAGS);
	checkException(env, __LINE__);
	jfieldID flagsID = (*env) -> GetFieldID(env, jinfoClass, "sinfo_flags", JAVA_SCTP_SINFOFLAGS_JNI);
	checkException(env, __LINE__);
	jobject flags = (*env)->GetObjectField(env, jinfo, flagsID);
	jlong flag = getLongField(env, flags, f, "flags");
	checkException(env, __LINE__);

	struct sctp_sndrcvinfo info;
	memset((void*) &info, 0, sizeof(info));
	info.sinfo_assoc_id = sinfo_assoc_id;
	info.sinfo_stream = sinfo_stream;
	info.sinfo_ssn = sinfo_ssn;
	info.sinfo_ppid = sinfo_ppid;
	info.sinfo_context = sinfo_context;
	info.sinfo_timetolive = sinfo_timetolive;
	info.sinfo_tsn = sinfo_tsn;
	info.sinfo_cumtsn = sinfo_cumtsn;
	info.sinfo_flags = flag;

	socklen_t sz = sizeof(info);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_DEFAULT_SEND_PARAM, &info, sz)) {
		throwIOException(env, "Set SCTP_DEFAULT_SEND_PARAM failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1EVENTS(JNIEnv* env, jclass this, jint fd) {
	struct sctp_event_subscribe events;
	memset((void*) &events, 0, sizeof(events));
	socklen_t sz = sizeof(events);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_EVENTS, &events, &sz)) {
		throwIOException(env, "Get SCTP_EVENTS failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_EVENTSUBSCRIBE);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(ZZZZZZZZZZ)V");
	checkException(env, __LINE__);

	jobject jevents = (*env) -> NewObject(env, cls, midInit, int2jboolean(events.sctp_data_io_event),
															 int2jboolean(events.sctp_association_event),
															 int2jboolean(events.sctp_address_event),
															 int2jboolean(events.sctp_send_failure_event),
															 int2jboolean(events.sctp_peer_error_event),
															 int2jboolean(events.sctp_shutdown_event),
															 int2jboolean(events.sctp_partial_delivery_event),
															 int2jboolean(events.sctp_adaptation_layer_event),
															 int2jboolean(events.sctp_authentication_event),
															 int2jboolean(events.sctp_sender_dry_event));
	return jevents;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1EVENTS(JNIEnv* env, jclass this, jint fd, jobject jevents) {
	jclass jeventsClass = (*env) -> GetObjectClass(env, jevents);
	checkException(env, __LINE__);

	int sctp_data_io_event = getBooleanField(env, jevents, jeventsClass, "sctp_data_io_event");
	int sctp_association_event = getBooleanField(env, jevents, jeventsClass, "sctp_association_event");
	int sctp_address_event = getBooleanField(env, jevents, jeventsClass, "sctp_address_event");
	int sctp_send_failure_event = getBooleanField(env, jevents, jeventsClass, "sctp_send_failure_event");
	int sctp_peer_error_event = getBooleanField(env, jevents, jeventsClass, "sctp_peer_error_event");
	int sctp_shutdown_event = getBooleanField(env, jevents, jeventsClass, "sctp_shutdown_event");
	int sctp_partial_delivery_event = getBooleanField(env, jevents, jeventsClass, "sctp_partial_delivery_event");
	int sctp_adaptation_layer_event = getBooleanField(env, jevents, jeventsClass, "sctp_adaptation_layer_event");
	int sctp_authentication_event = getBooleanField(env, jevents, jeventsClass, "sctp_authentication_event");
	int sctp_sender_dry_event = getBooleanField(env, jevents, jeventsClass, "sctp_sender_dry_event");
	checkException(env, __LINE__);

	struct sctp_event_subscribe events;
	memset((void*) &events, 0, sizeof(events));

	events.sctp_data_io_event = sctp_data_io_event;
	events.sctp_association_event = sctp_association_event;
	events.sctp_address_event = sctp_address_event;
	events.sctp_send_failure_event = sctp_send_failure_event;
	events.sctp_peer_error_event = sctp_peer_error_event;
	events.sctp_shutdown_event = sctp_shutdown_event;
	events.sctp_partial_delivery_event = sctp_partial_delivery_event;
	events.sctp_adaptation_layer_event = sctp_adaptation_layer_event;
	events.sctp_authentication_event = sctp_authentication_event;
	events.sctp_sender_dry_event = sctp_sender_dry_event;

	socklen_t sz = sizeof(events);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_EVENTS, &events, sz)) {
		throwIOException(env, "Set SCTP_EVENTS failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jboolean JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1I_1WANT_1MAPPED_1V4_1ADDR(JNIEnv* env, jclass this, jint fd) {
	int v4addr;
	socklen_t sz = sizeof(v4addr);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_I_WANT_MAPPED_V4_ADDR, &v4addr, &sz)) {
		throwIOException(env, "Get SCTP_I_WANT_MAPPED_V4_ADDR failed: %s", strerror(errno));
		return JNI_FALSE;
	}
	checkException(env, __LINE__);

	return int2jboolean(v4addr);
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1I_1WANT_1MAPPED_1V4_1ADDR(JNIEnv* env, jclass this, jint fd, jboolean v4addr) {
	int v4 = jboolean2int(v4addr);
	socklen_t sz = sizeof(v4);
	if(setsockopt(fd, IPPROTO_SCTP, SCTP_I_WANT_MAPPED_V4_ADDR, &v4, sz)) {
		throwIOException(env, "Set SCTP_I_WANT_MAPPED_V4_ADDR failed: %s", strerror(errno));
		return;
	}

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1MAXSEG(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_assoc_value val;
	val.assoc_id = associd;
	memset((void*) &val, 0, sizeof(val));
	socklen_t sz = sizeof(val);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_MAXSEG, &val, &sz)) {
		throwIOException(env, "Get SCTP_MAXSEG failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_ASSOCVALUE);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(IJ)V");
	checkException(env, __LINE__);

	return (*env) -> NewObject(env, cls, midInit, val.assoc_id, (unsigned long) val.assoc_value);
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1MAXSEG(JNIEnv* env, jclass this, jint fd, jobject maxseg) {
	jclass jmaxsegClass = (*env) -> GetObjectClass(env, maxseg);
	checkException(env, __LINE__);

	jint assoc_id = getIntField(env, maxseg, jmaxsegClass, "assoc_id");
	jlong assoc_value = getLongField(env, maxseg, jmaxsegClass, "assoc_value");
	checkException(env, __LINE__);

	struct sctp_assoc_value val;
	memset((void*) &val, 0, sizeof(val));

	val.assoc_id = assoc_id;
	val.assoc_value = assoc_value;

	socklen_t sz = sizeof(val);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_MAXSEG, &val, sz)) {
		throwIOException(env, "Set SCTP_MAXSEG failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1STATUS(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_status status;
	status.sstat_assoc_id = associd;
	memset((void*) &status, 0, sizeof(status));
	socklen_t sz = sizeof(status);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_STATUS, &status, &sz)) {
		throwIOException(env, "Get SCTP_STATUS failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_STATUS);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", JAVA_SCTP_STATUS_INIT);
	checkException(env, __LINE__);

	int sstat_assoc_id = status.sstat_assoc_id;
	long sstat_rwnd = status.sstat_rwnd;
	int sstat_unackdata = status.sstat_unackdata;
	int sstat_penddata = status.sstat_penddata;
	int sstat_instrms = status.sstat_instrms;
	int sstat_outstrms = status.sstat_outstrms;
	long sstat_fragmentation_point = status.sstat_fragmentation_point;

	jobject sstat_state =
			(status.sstat_state == SCTP_CLOSED) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_CLOSED") :
			(status.sstat_state == SCTP_COOKIE_WAIT) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_COOKIE_WAIT") :
			(status.sstat_state == SCTP_COOKIE_ECHOED) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_COOKIE_ECHOED") :
			(status.sstat_state == SCTP_ESTABLISHED) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_ESTABLISHED") :
			(status.sstat_state == SCTP_SHUTDOWN_PENDING) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_SHUTDOWN_PENDING") :
			(status.sstat_state == SCTP_SHUTDOWN_SENT) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_SHUTDOWN_SENT") :
			(status.sstat_state == SCTP_SHUTDOWN_RECEIVED) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_SHUTDOWN_RECEIVED") :
			(status.sstat_state == SCTP_SHUTDOWN_ACK_SENT) ? getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_SHUTDOWN_ACK_SENT") :
			getEnum(env, JAVA_SCTP_SSTATSTATE, "SCTP_EMPTY");
	checkException(env, __LINE__);

	jclass paddrcls = (*env) -> FindClass(env, JAVA_SCTP_PADDRINFO);
	checkException(env, __LINE__);
	jmethodID paddrmid = (*env) -> GetMethodID(env, paddrcls, "<init>", JAVA_SCTP_PADDRINFO_INIT);
	checkException(env, __LINE__);

	int spinfo_assoc_id = status.sstat_primary.spinfo_assoc_id;
	long spinfo_cwnd = status.sstat_primary.spinfo_cwnd;
	long spinfo_srtt = status.sstat_primary.spinfo_srtt;
	long spinfo_rto = status.sstat_primary.spinfo_rto;
	long spinfo_mtu = status.sstat_primary.spinfo_mtu;

	jobject spinfo_state =
			(status.sstat_primary.spinfo_state == SCTP_INACTIVE) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_INACTIVE") :
			(status.sstat_primary.spinfo_state == SCTP_PF) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_PF") :
			(status.sstat_primary.spinfo_state == SCTP_ACTIVE) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_ACTIVE") :
			(status.sstat_primary.spinfo_state == SCTP_UNCONFIRMED) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_UNCONFIRMED") :
			getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_UNKNOWN");

	jobject spinfo_address = sockAddrToInetSocketAddress(env, &status.sstat_primary.spinfo_address);
	checkException(env, __LINE__);

	jobject sstat_primary = (*env) -> NewObject(env, paddrcls, paddrmid, spinfo_assoc_id, spinfo_address, 
																						  (unsigned long) spinfo_state, 
																						  (unsigned long) spinfo_cwnd,
																		 				  (unsigned long) spinfo_srtt, 
																						  (unsigned long) spinfo_rto, 
																						  (unsigned long) spinfo_mtu);
	checkException(env, __LINE__);

	return (*env) -> NewObject(env, cls, midInit, sstat_assoc_id, 
												  (unsigned long) sstat_state, 
												  (unsigned long) sstat_rwnd, 
												  sstat_unackdata, 
												  sstat_penddata,
												  sstat_instrms, 
												  sstat_outstrms, 
												  (unsigned long) sstat_fragmentation_point, 
												  sstat_primary);
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1PEER_1ADDR_1INFO(JNIEnv* env, jclass this, jint fd, jint associd, jobject peer) {

	struct sctp_paddrinfo info;
	memset((void*) &info, 0, sizeof(info));

	info.spinfo_assoc_id = associd;
	info.spinfo_address = InetSocketAddressToSockAddr(env, peer, Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1I_1WANT_1MAPPED_1V4_1ADDR(env, this, fd));

	socklen_t sz = sizeof(info);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_GET_PEER_ADDR_INFO, &info, &sz)) {
		throwIOException(env, "Get SCTP_GET_PEER_ADDR_INFO failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass paddrcls = (*env) -> FindClass(env, JAVA_SCTP_PADDRINFO);
	checkException(env, __LINE__);
	jmethodID paddrmid = (*env) -> GetMethodID(env, paddrcls, "<init>", JAVA_SCTP_PADDRINFO_INIT);
	checkException(env, __LINE__);

	int spinfo_assoc_id = info.spinfo_assoc_id;
	long spinfo_cwnd = info.spinfo_cwnd;
	long spinfo_srtt = info.spinfo_srtt;
	long spinfo_rto = info.spinfo_rto;
	long spinfo_mtu = info.spinfo_mtu;

	jobject spinfo_state =
			(info.spinfo_state == SCTP_INACTIVE) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_INACTIVE") :
			(info.spinfo_state == SCTP_PF) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_PF") :
			(info.spinfo_state == SCTP_ACTIVE) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_ACTIVE") :
			(info.spinfo_state == SCTP_UNCONFIRMED) ? getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_UNCONFIRMED") :
			getEnum(env, JAVA_SCTP_SPINFOSTATE, "SCTP_UNKNOWN");

	jobject spinfo_address = sockAddrToInetSocketAddress(env, &info.spinfo_address);
	checkException(env, __LINE__);

	return (*env) -> NewObject(env, paddrcls, paddrmid, spinfo_assoc_id, spinfo_address, 
																		 (unsigned long) spinfo_state, 
																		 (unsigned long) spinfo_cwnd,
																		 (unsigned long) spinfo_srtt, 
																		 (unsigned long) spinfo_rto, 
																		 (unsigned long) spinfo_mtu);
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1DELAYED_1SACK(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_sack_info info;
	memset((void*) &info, 0, sizeof(info));

	info.sack_assoc_id = associd;

	socklen_t sz = sizeof(info);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_DELAYED_SACK, &info, &sz)) {
		throwIOException(env, "Get SCTP_DELAYED_SACK failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass sackcls = (*env) -> FindClass(env, JAVA_SCTP_SACKINFO);
	checkException(env, __LINE__);
	jmethodID sackmid = (*env) -> GetMethodID(env, sackcls, "<init>", "(IJJ)V");
	checkException(env, __LINE__);

	int sack_assoc_id = info.sack_assoc_id;
	long sack_delay = info.sack_delay;
	long sack_freq = info.sack_freq;

	return (*env) -> NewObject(env, sackcls, sackmid, sack_assoc_id, 
													  (unsigned long) sack_delay, 
													  (unsigned long) sack_freq);
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1DELAYED_1SACK(JNIEnv* env, jclass this, jint fd, jobject jsack) {
	jclass jsackClass = (*env) -> GetObjectClass(env, jsack);
	checkException(env, __LINE__);

	jint sack_assoc_id = getIntField(env, jsack, jsackClass, "sack_assoc_id");
	jlong sack_delay = getLongField(env, jsack, jsackClass, "sack_delay");
	jlong sack_freq = getLongField(env, jsack, jsackClass, "sack_freq");
	checkException(env, __LINE__);

	struct sctp_sack_info info;
	memset((void*) &info, 0, sizeof(info));

	info.sack_assoc_id = sack_assoc_id;
	info.sack_delay = sack_delay;
	info.sack_freq = sack_freq;

	socklen_t sz = sizeof(info);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_DELAYED_SACK, &info, sz)) {
		throwIOException(env, "Set SCTP_DELAYED_SACK failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1CONTEXT(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_assoc_value val;
	val.assoc_id = associd;
	memset((void*) &val, 0, sizeof(val));
	socklen_t sz = sizeof(val);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_CONTEXT, &val, &sz)) {
		throwIOException(env, "Get SCTP_CONTEXT failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_ASSOCVALUE);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(IJ)V");
	checkException(env, __LINE__);

	return (*env) -> NewObject(env, cls, midInit, val.assoc_id, (unsigned long) val.assoc_value);
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1CONTEXT(JNIEnv* env, jclass this, jint fd, jobject jval) {
	jclass jcontextClass = (*env) -> GetObjectClass(env, jval);
	checkException(env, __LINE__);

	jint assoc_id = getIntField(env, jval, jcontextClass, "assoc_id");
	jlong assoc_value = getLongField(env, jval, jcontextClass, "assoc_value");
	checkException(env, __LINE__);

	struct sctp_assoc_value val;
	memset((void*) &val, 0, sizeof(val));

	val.assoc_id = assoc_id;
	val.assoc_value = assoc_value;

	socklen_t sz = sizeof(val);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_CONTEXT, &val, sz)) {
		throwIOException(env, "Set SCTP_CONTEXT failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jboolean JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1FRAGMENT_1INTERLEAVE(JNIEnv* env, jclass this, jint fd) {
	int interleave;
	socklen_t sz = sizeof(interleave);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_FRAGMENT_INTERLEAVE, &interleave, &sz)) {
		throwIOException(env, "Get SCTP_FRAGMENT_INTERLEAVE failed: %s", strerror(errno));
		return -1;
	}
	checkException(env, __LINE__);

	return interleave == 0 ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1FRAGMENT_1INTERLEAVE(JNIEnv* env, jclass this, jint fd, jboolean jinterleave) {
	int interleave = jinterleave == JNI_FALSE ? 0 : 1;
	socklen_t sz = sizeof(interleave);
	if(setsockopt(fd, IPPROTO_SCTP, SCTP_FRAGMENT_INTERLEAVE, &interleave, sz)) {
		throwIOException(env, "Set SCTP_FRAGMENT_INTERLEAVE failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jboolean JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1REUSE_1ADDR(JNIEnv* env, jclass this, jint fd) {
	int reuse;
	socklen_t sz = sizeof(reuse);

	if(getsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, &sz)) {
		throwIOException(env, "Get SO_REUSEADDR failed: %s", strerror(errno));
		return -1;
	}
	checkException(env, __LINE__);

	return reuse == 0 ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1REUSE_1ADDR(JNIEnv* env, jclass this, jint fd, jboolean jreuse) {
	int reuse = jreuse == JNI_FALSE ? 0 : 1;
	socklen_t sz = sizeof(reuse);
	if(setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, sz)) {
		throwIOException(env, "Get SO_REUSEADDR failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jlong JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1PARTIAL_1DELIVERY_1POINT(JNIEnv* env, jclass this, jint fd) {
	u_int32_t deliveryPoint;
	socklen_t sz = sizeof(deliveryPoint);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_PARTIAL_DELIVERY_POINT, &deliveryPoint, &sz)) {
		throwIOException(env, "Get SCTP_PARTIAL_DELIVERY_POINT failed: %s", strerror(errno));
		return -1;
	}
	checkException(env, __LINE__);

	return deliveryPoint;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1PARTIAL_1DELIVERY_1POINT(JNIEnv* env, jclass this, jint fd, jlong deliveryPoint) {
	u_int32_t delivery = deliveryPoint;
	socklen_t sz = sizeof(delivery);
	if(setsockopt(fd, IPPROTO_SCTP, SCTP_PARTIAL_DELIVERY_POINT, &delivery, sz)) {
		throwIOException(env, "Set SCTP_PARTIAL_DELIVERY_POINT failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1MAX_1BURST(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_assoc_value val;
	val.assoc_id = associd;
	memset((void*) &val, 0, sizeof(val));
	socklen_t sz = sizeof(val);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_MAX_BURST, &val, &sz)) {
		throwIOException(env, "Get SCTP_MAX_BURST failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_ASSOCVALUE);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(IJ)V");
	checkException(env, __LINE__);

	return (*env) -> NewObject(env, cls, midInit, val.assoc_id, (unsigned long) val.assoc_value);
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1MAX_1BURST(JNIEnv* env, jclass this, jint fd, jobject jval) {
	jclass jcontextClass = (*env) -> GetObjectClass(env, jval);
	checkException(env, __LINE__);

	jint assoc_id = getIntField(env, jval, jcontextClass, "assoc_id");
	jlong assoc_value = getLongField(env, jval, jcontextClass, "assoc_value");
	checkException(env, __LINE__);

	struct sctp_assoc_value val;
	memset((void*) &val, 0, sizeof(val));

	val.assoc_id = assoc_id;
	val.assoc_value = assoc_value;

	socklen_t sz = sizeof(val);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_MAX_BURST, &val, sz)) {
		throwIOException(env, "Get SCTP_MAX_BURST failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1AUTH_1CHUNK(JNIEnv* env, jclass this, jint fd, jobject authchunk) {
	jclass jauthClass = (*env) -> GetObjectClass(env, authchunk);
	checkException(env, __LINE__);

	jfieldID authChunkID = (*env) -> GetFieldID(env, jauthClass, "sauth_chunk", JAVA_SCTP_CIDT_JNI);
	checkException(env, __LINE__);
	jobject sauth_chunk = (*env)->GetObjectField(env, authchunk, authChunkID);
	checkException(env, __LINE__);

	struct sctp_authchunk auth;
	memset((void*) &auth, 0, sizeof(auth));

	char* enumVal = getEnumValue(env, sauth_chunk, JAVA_SCTP_CIDT);
	if (strcmp(enumVal, "SCTP_CID_DATA") == 0) {
		auth.sauth_chunk = SCTP_CID_DATA;
	} else if(strcmp(enumVal, "SCTP_CID_INIT") == 0) {
		auth.sauth_chunk = SCTP_CID_INIT;
	} else if(strcmp(enumVal, "SCTP_CID_INIT_ACK") == 0) {
		auth.sauth_chunk = SCTP_CID_INIT_ACK;
	} else if(strcmp(enumVal, "SCTP_CID_SACK") == 0) {
		auth.sauth_chunk = SCTP_CID_SACK;
	} else if(strcmp(enumVal, "SCTP_CID_HEARTBEAT") == 0) {
		auth.sauth_chunk = SCTP_CID_HEARTBEAT;
	} else if(strcmp(enumVal, "SCTP_CID_HEARTBEAT_ACK") == 0) {
		auth.sauth_chunk = SCTP_CID_HEARTBEAT_ACK;
	} else if(strcmp(enumVal, "SCTP_CID_ABORT") == 0) {
		auth.sauth_chunk = SCTP_CID_ABORT;
	} else if(strcmp(enumVal, "SCTP_CID_SHUTDOWN") == 0) {
		auth.sauth_chunk = SCTP_CID_SHUTDOWN;
	} else if(strcmp(enumVal, "SCTP_CID_SHUTDOWN_ACK") == 0) {
		auth.sauth_chunk = SCTP_CID_SHUTDOWN_ACK;
	} else if(strcmp(enumVal, "SCTP_CID_ERROR") == 0) {
		auth.sauth_chunk = SCTP_CID_ERROR;
	} else if(strcmp(enumVal, "SCTP_CID_COOKIE_ECHO") == 0) {
		auth.sauth_chunk = SCTP_CID_COOKIE_ECHO;
	} else if(strcmp(enumVal, "SCTP_CID_COOKIE_ACK") == 0) {
		auth.sauth_chunk = SCTP_CID_COOKIE_ACK;
	} else if(strcmp(enumVal, "SCTP_CID_ECN_ECNE") == 0) {
		auth.sauth_chunk = SCTP_CID_ECN_ECNE;
	} else if(strcmp(enumVal, "SCTP_CID_ECN_CWR") == 0) {
		auth.sauth_chunk = SCTP_CID_ECN_CWR;
	} else if(strcmp(enumVal, "SCTP_CID_SHUTDOWN_COMPLETE") == 0) {
		auth.sauth_chunk = SCTP_CID_SHUTDOWN_COMPLETE;
	} else if(strcmp(enumVal, "SCTP_CID_AUTH") == 0) {
		auth.sauth_chunk = SCTP_CID_AUTH;
	} else if(strcmp(enumVal, "SCTP_CID_FWD_TSN") == 0) {
		auth.sauth_chunk = SCTP_CID_FWD_TSN;
	} else if(strcmp(enumVal, "SCTP_CID_ASCONF") == 0) {
		auth.sauth_chunk = SCTP_CID_ASCONF;
	} else if(strcmp(enumVal, "SCTP_CID_ASCONF_ACK") == 0) {
		auth.sauth_chunk = SCTP_CID_ASCONF_ACK;
	} else if(strcmp(enumVal, "SCTP_CID_RECONF") == 0) {
		auth.sauth_chunk = SCTP_CID_RECONF;
	}

	socklen_t sz = sizeof(auth);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_CHUNK, &auth, sz)) {
		throwIOException(env, "Set SCTP_AUTH_CHUNK failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1HMAC_1IDENT(JNIEnv* env, jclass this, jint fd) {
	struct sctp_hmacalgo algo;
	memset((void*) &algo, 0, sizeof(algo));
	socklen_t sz = sizeof(algo) + 64;

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_HMAC_IDENT, &algo, &sz)) {
		throwIOException(env, "Get SCTP_HMAC_IDENT failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_HMACALGO);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", JAVA_SCTP_HMACALGO_INIT);
	checkException(env, __LINE__);

	jsize length = algo.shmac_number_of_idents;
	jclass enumClass = (*env) -> FindClass(env, JAVA_SCTP_HMACALGOIDENTS);
	jobjectArray shmac_idents = (*env) -> NewObjectArray(env, length, enumClass, NULL);
	int i;
	for(i = 0; i < length; i++) {

		jobject ident =
			(algo.shmac_idents[i] == SCTP_AUTH_HMAC_ID_SHA1) ? getEnum(env, JAVA_SCTP_HMACALGOIDENTS, "SCTP_AUTH_HMAC_ID_SHA1") :
			getEnum(env, JAVA_SCTP_HMACALGOIDENTS, "SCTP_AUTH_HMAC_ID_SHA256");

		(*env) -> SetObjectArrayElement(env, shmac_idents, i, ident);
	}

	return (*env) -> NewObject(env, cls, midInit, (unsigned long) algo.shmac_number_of_idents, 
												  shmac_idents);
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1HMAC_1IDENT(JNIEnv* env, jclass this, jint fd, jobject hmac) {
	jclass jhmacClass = (*env) -> GetObjectClass(env, hmac);
	checkException(env, __LINE__);

	jlong shmac_num_idents = getLongField(env, hmac, jhmacClass, "shmac_num_idents");
	jobjectArray shmac_idents = (jobjectArray) getArrayField(env, hmac, jhmacClass, JAVA_SCTP_HMACALGOIDENTS_JNI, "shmac_idents");
	checkException(env, __LINE__);

	struct sctp_hmacalgo* val =
			(struct sctp_hmacalgo*) malloc(sizeof(struct sctp_hmacalgo) + shmac_num_idents * sizeof(__u16));

	int i;
	for(i = 0; i < shmac_num_idents; i++) {

		jobject enumObj = (*env) -> GetObjectArrayElement(env, shmac_idents, i);
		char* enumVal = getEnumValue(env, enumObj, JAVA_SCTP_HMACALGOIDENTS);

		if (strcmp(enumVal, "SCTP_AUTH_HMAC_ID_SHA1") == 0) {
			val->shmac_idents[i] = SCTP_AUTH_HMAC_ID_SHA1;
		} else {
			val->shmac_idents[i] = SCTP_AUTH_HMAC_ID_SHA256;
		}
	}

	val -> shmac_number_of_idents = shmac_num_idents;

	socklen_t sz = sizeof(struct sctp_hmacalgo) + shmac_num_idents * sizeof(__u16);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_HMAC_IDENT, val, sz)) {
		throwIOException(env, "Set SCTP_HMAC_IDENT failed: %s", strerror(errno));
		free(val);
		return;
	}
	checkException(env, __LINE__);
	free(val);
	return;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1AUTH_1KEY(JNIEnv* env, jclass this, jint fd, jobject key) {
	jclass jkeyClass = (*env) -> GetObjectClass(env, key);
	checkException(env, __LINE__);

	jint sca_assoc_id = getIntField(env, key, jkeyClass, "sca_assoc_id");
	jint sca_keynumber = getIntField(env, key, jkeyClass, "sca_keynumber");
	jint sca_keylength = getIntField(env, key, jkeyClass, "sca_keylength");
	jobjectArray sca_key = (jbyteArray) getArrayField(env, key, jkeyClass, "[B", "sca_key");
	checkException(env, __LINE__);

	struct sctp_authkey* val =
			(struct sctp_authkey*) malloc(sizeof(struct sctp_authkey) + sca_keylength * sizeof(__u8));

	jbyte* arr = (*env) -> GetByteArrayElements(env, sca_key, JNI_FALSE);
	
	int i;
	for(i = 0; i < sca_keylength; i++) {
		val -> sca_key[i] = (__u8) arr[i];
	}
	(*env) -> ReleaseByteArrayElements(env, sca_key, arr, 0);

	val -> sca_assoc_id = sca_assoc_id;
	val -> sca_keynumber = sca_keynumber;
	val -> sca_keylength = sca_keylength;

	socklen_t sz = sizeof(struct sctp_authkey) + sca_keylength * sizeof(__u16);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_KEY, val, sz)) {
		throwIOException(env, "Set SCTP_AUTH_KEY failed: %s", strerror(errno));
		free(val);
		return;
	}
	checkException(env, __LINE__);
	free(val);
	return;
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1AUTH_1ACTIVE_1KEY(JNIEnv* env, jclass this, jint fd, jint associd) {
	struct sctp_authkeyid val;
	val.scact_assoc_id = associd;
	memset((void*) &val, 0, sizeof(val));
	socklen_t sz = sizeof(val);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_ACTIVE_KEY, &val, &sz)) {
		throwIOException(env, "Get SCTP_AUTh_ACTIVE_KEY failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_AUTHKEYID);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", "(II)V");
	checkException(env, __LINE__);

	return (*env) -> NewObject(env, cls, midInit, val.scact_assoc_id, val.scact_keynumber);
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1AUTH_1ACTIVE_1KEY(JNIEnv* env, jclass this, jint fd, jobject key) {
	jclass jkeyClass = (*env) -> GetObjectClass(env, key);
	checkException(env, __LINE__);

	jint scact_assoc_id = getIntField(env, key, jkeyClass, "scact_assoc_id");
	jint scact_keynumber = getIntField(env, key, jkeyClass, "scact_keynumber");
	checkException(env, __LINE__);

	struct sctp_authkeyid val;
	memset((void*) &val, 0, sizeof(val));

	val.scact_assoc_id = scact_assoc_id;
	val.scact_keynumber = scact_keynumber;

	socklen_t sz = sizeof(struct sctp_authkeyid);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_ACTIVE_KEY, &val, sz)) {
		throwIOException(env, "Set SCTP_AUTH_ACTIVE_KEY failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

JNIEXPORT void JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1setSCTP_1AUTH_1DELETE_1KEY(JNIEnv* env, jclass this, jint fd, jobject key) {
	jclass jkeyClass = (*env) -> GetObjectClass(env, key);
	checkException(env, __LINE__);

	jint scact_assoc_id = getIntField(env, key, jkeyClass, "scact_assoc_id");
	jint scact_keynumber = getIntField(env, key, jkeyClass, "scact_keynumber");
	checkException(env, __LINE__);

	struct sctp_authkeyid val;
	memset((void*) &val, 0, sizeof(val));

	val.scact_assoc_id = scact_assoc_id;
	val.scact_keynumber = scact_keynumber;

	socklen_t sz = sizeof(struct sctp_authkeyid);

	if(setsockopt(fd, IPPROTO_SCTP, SCTP_AUTH_DELETE_KEY, &val, sz)) {
		throwIOException(env, "Set SCTP_AUTH_DELETE_KEY failed: %s", strerror(errno));
		return;
	}
	checkException(env, __LINE__);

	return;
}

jobject getChunkEnum(JNIEnv* env, uint8_t cid) {
	return
	(cid == SCTP_CID_DATA) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_DATA") :
	(cid == SCTP_CID_INIT) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_INIT") :
	(cid == SCTP_CID_INIT_ACK) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_INIT ACK") :
	(cid == SCTP_CID_HEARTBEAT) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_HEARTBEAT") :
	(cid == SCTP_CID_HEARTBEAT_ACK) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_HEARTBEAT_ACK") :
	(cid == SCTP_CID_ABORT) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_ABORT") :
	(cid == SCTP_CID_SHUTDOWN) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_SHUTDOWN") :
	(cid == SCTP_CID_SHUTDOWN_ACK) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_SHUTDOWN_ACK") :
	(cid == SCTP_CID_ERROR) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_ERROR") :
	(cid == SCTP_CID_COOKIE_ECHO) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_COOKIE_ECHO") :
	(cid == SCTP_CID_COOKIE_ACK) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_COOKIE_ACK") :
	(cid == SCTP_CID_ECN_ECNE) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_ECN_ECNE") :
	(cid == SCTP_CID_ECN_CWR) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_ECN_CWR") :
	(cid == SCTP_CID_SHUTDOWN_COMPLETE) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_SHUTDOWN_COMPLETE") :
	(cid == SCTP_CID_AUTH) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_AUTH") :
	(cid == SCTP_CID_FWD_TSN) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_FWD_TSN") :
	(cid == SCTP_CID_ASCONF) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_ASCONF") :
	(cid == SCTP_CID_ASCONF_ACK) ? getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_ASCONF_ACK") :
	getEnum(env, JAVA_SCTP_CIDT, "SCTP_CID_RECONF");
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1PEER_1AUTH_1CHUNKS(JNIEnv* env, jclass this, jint fd, jint assocID) {
	struct sctp_authchunks chunks;
	memset((void*) &chunks, 0, sizeof(chunks));
	socklen_t sz = sizeof(chunks) + 64;

	chunks.gauth_assoc_id = assocID;

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_PEER_AUTH_CHUNKS, &chunks, &sz)) {
		throwIOException(env, "Get SCTP_PEER_AUTH_CHUNKS failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_AUTHCHUNKS);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", JAVA_SCTP_AUTHCHUNKS_INIT);
	checkException(env, __LINE__);

	jsize length = chunks.gauth_number_of_chunks;
	jclass enumClass = (*env) -> FindClass(env, JAVA_SCTP_CIDT);
	jobjectArray gauth_chunks = (*env) -> NewObjectArray(env, length, enumClass, NULL);
	int i = 0;
	for(i = 0; i < length; i++) {
		jobject chunk = getChunkEnum(env, chunks.gauth_chunks[i]);
		(*env) -> SetObjectArrayElement(env, gauth_chunks, i, chunk);
	}

	return (*env) -> NewObject(env, cls, midInit, chunks.gauth_assoc_id, (unsigned long) chunks.gauth_number_of_chunks, 
																		 gauth_chunks);
}

JNIEXPORT jobject JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1LOCAL_1AUTH_1CHUNKS(JNIEnv* env, jclass this, jint fd, jint assocID) {
	struct sctp_authchunks chunks;
	memset((void*) &chunks, 0, sizeof(chunks));
	socklen_t sz = sizeof(chunks) + 64;

	chunks.gauth_assoc_id = assocID;

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_LOCAL_AUTH_CHUNKS, &chunks, &sz)) {
		throwIOException(env, "Get SCTP_LOCAL_AUTH_CHUNKS failed: %s", strerror(errno));
		return NULL;
	}
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, JAVA_SCTP_AUTHCHUNKS);
	checkException(env, __LINE__);
	jmethodID midInit = (*env) -> GetMethodID(env, cls, "<init>", JAVA_SCTP_AUTHCHUNKS_INIT);
	checkException(env, __LINE__);

	jsize length = chunks.gauth_number_of_chunks;
	jclass enumClass = (*env) -> FindClass(env, JAVA_SCTP_CIDT);
	jobjectArray gauth_chunks = (*env) -> NewObjectArray(env, length, enumClass, NULL);
	int i = 0;
	for(i = 0; i < length; i++) {
		jobject chunk = getChunkEnum(env, chunks.gauth_chunks[i]);
		(*env) -> SetObjectArrayElement(env, gauth_chunks, i, chunk);
	}

	return (*env) -> NewObject(env, cls, midInit, chunks.gauth_assoc_id, chunks.gauth_number_of_chunks, gauth_chunks);
}

JNIEXPORT jlong JNICALL Java_alcatel_tess_hometop_gateways_reactor_impl_SctpSocketOptionHelper_n_1getSCTP_1GET_1ASSOC_1NUMBER(JNIEnv* env, jclass this, jint fd) {
	u_int32_t assocNumber;
	socklen_t sz = sizeof(assocNumber);

	if(getsockopt(fd, IPPROTO_SCTP, SCTP_GET_ASSOC_NUMBER, &assocNumber, &sz)) {
		throwIOException(env, "Get SCTP_GET_ASSOC_NUMBER failed: %s", strerror(errno));
		return -1;
	}
	checkException(env, __LINE__);

	return assocNumber;
}
