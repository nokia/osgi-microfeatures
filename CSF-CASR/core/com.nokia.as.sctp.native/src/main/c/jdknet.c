
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/utsname.h>
#include <netinet/in.h>

#include <linux/types.h>
typedef __u64 u64;
typedef __u32 u32;
typedef __u16 u16;
typedef __u8 u8;

#include <linux/ipv6_route.h>
#include <linux/route.h>
#include <arpa/inet.h>
#include "jdknet.h"

#define IPv4 1
#define IPv6 2

static void checkException(JNIEnv* env, int line) {
	if((*env) -> ExceptionCheck(env)) {
		(*env) -> ExceptionDescribe(env);
		fprintf(stderr, "Java exception in jdknet.c at line %d\n", line);
		abort();
	}
}

/**************************************** sockAddr to InetAddress ***********************************/

jboolean IPv6_supported(){

    char ch;
    char* file_name = "/proc/sys/net/ipv6/conf/all/disable_ipv6";
    FILE *fp = fopen(file_name, "r"); // read mode

    if (fp == NULL) {
      return JNI_FALSE;
    }

    ch = fgetc(fp);
    fclose(fp);
    
    int x = ch - '0';
    return x ? JNI_FALSE : JNI_TRUE;
}

//Taken from JDK
int NET_IsIPv4Mapped(jbyte* caddr) {
    int i;
    for (i = 0; i < 10; i++) {
        if (caddr[i] != 0x00) {
            return 0; /* false */
        }
    }

    if (((caddr[10] & 0xff) == 0xff) && ((caddr[11] & 0xff) == 0xff)) {
        return 1; /* true */
    }
    return 0; /* false */
}

//Taken from JDK
int NET_IPv4MappedToIPv4(jbyte* caddr) {
    return ((caddr[12] & 0xff) << 24) | ((caddr[13] & 0xff) << 16) | ((caddr[14] & 0xff) << 8)
        | (caddr[15] & 0xff);
}

//Adapted from JDK
void setInetAddress_family(JNIEnv *env, jobject iaObj, int family) {
	jclass c = (*env)->FindClass(env,"java/net/InetAddress");
	checkException(env, __LINE__);
	jclass h = (*env) -> FindClass(env, "java/net/InetAddress$InetAddressHolder");
	checkException(env, __LINE__);
	jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder", "Ljava/net/InetAddress$InetAddressHolder;");
	checkException(env, __LINE__);
    jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
    checkException(env, __LINE__);
    jfieldID iac_familyID = (*env) -> GetFieldID(env, h, "family", "I");
    checkException(env, __LINE__);
    (*env) -> SetIntField(env, holder, iac_familyID, family);
}

//Adapted from JDK
void setInetAddress_addr(JNIEnv *env, jobject iaObj, int address) {
	jclass c = (*env)->FindClass(env,"java/net/InetAddress");
	checkException(env, __LINE__);
	jclass h = (*env) -> FindClass(env, "java/net/InetAddress$InetAddressHolder");
	checkException(env, __LINE__);
	jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder", "Ljava/net/InetAddress$InetAddressHolder;");
	checkException(env, __LINE__);
    jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
    checkException(env, __LINE__);
    jfieldID iac_addressID = (*env) -> GetFieldID(env, h, "address", "I");
    checkException(env, __LINE__);
    (*env) -> SetIntField(env, holder, iac_addressID, address);
}

//Adapted from JDK
jboolean setInet6Address_scopeid(JNIEnv* env, jobject iaObj, int scopeid) {
	jclass c = (*env)->FindClass(env,"java/net/Inet6Address");
	checkException(env, __LINE__);
	if(c == NULL) return JNI_FALSE;
	jclass h = (*env) -> FindClass(env, "java/net/Inet6Address$Inet6AddressHolder");
	checkException(env, __LINE__);
	if(h == NULL) return JNI_FALSE;
	jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder6", "Ljava/net/Inet6Address$Inet6AddressHolder;");
	checkException(env, __LINE__);
	if(ia_holderID == NULL) return JNI_FALSE;
	jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
	checkException(env, __LINE__);
    if(holder == NULL) return JNI_FALSE;
    jfieldID ia6_scopeidID = (*env) -> GetFieldID(env, h, "scope_id", "I");
    checkException(env, __LINE__);
    if(ia6_scopeidID == NULL) return JNI_FALSE;
    (*env) -> SetIntField(env, holder, ia6_scopeidID, scopeid);
    checkException(env, __LINE__);
    if (scopeid > 0) {
    	jfieldID ia6_scopeidsetID = (*env) -> GetFieldID(env, h, "scope_id_set", "Z");
    	checkException(env, __LINE__);
        (*env) -> SetBooleanField(env, holder, ia6_scopeidsetID, JNI_TRUE);
        checkException(env, __LINE__);
    }
    return JNI_TRUE;
}

//Adapted from JDK
jboolean setInet6Address_ipaddress(JNIEnv* env, jobject iaObj, char* address) {
    jbyteArray addr;
    jclass c = (*env)->FindClass(env,"java/net/Inet6Address");
    checkException(env, __LINE__);
    if(c == NULL) return JNI_FALSE;
    jclass h = (*env) -> FindClass(env, "java/net/Inet6Address$Inet6AddressHolder");
    checkException(env, __LINE__);
    if(h == NULL) return JNI_FALSE;
    jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder6", "Ljava/net/Inet6Address$Inet6AddressHolder;");
    checkException(env, __LINE__);
    if(ia_holderID == NULL) return JNI_FALSE;
    jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
    checkException(env, __LINE__);
    if(holder == NULL) return JNI_FALSE;
    jfieldID ia6_ipaddressID = (*env)->GetFieldID(env, h, "ipaddress", "[B");
    checkException(env, __LINE__);
    if(ia6_ipaddressID == NULL) return JNI_FALSE;
    addr =  (jbyteArray) (*env) -> GetObjectField(env, holder, ia6_ipaddressID);
    checkException(env, __LINE__);
    if (addr == NULL) {
        addr = (*env) -> NewByteArray(env, 16);
        checkException(env, __LINE__);
        if(addr == NULL) return JNI_FALSE;
        (*env) -> SetObjectField(env, holder, ia6_ipaddressID, addr);
        checkException(env, __LINE__);
    }
    (*env) -> SetByteArrayRegion(env, addr, 0, 16, (jbyte*) address);
    checkException(env, __LINE__);
    return JNI_TRUE;
}

//Adapted from JDK
jobject NET_SockaddrToInetAddress(JNIEnv* env, struct sockaddr_storage* him, int* port) {
    jobject iaObj;

    if (him -> ss_family == AF_INET6) {
    	struct sockaddr_in6* him6 = (struct sockaddr_in6*) him;
        jbyte* caddr = (jbyte*) & (him6 -> sin6_addr);
        if (NET_IsIPv4Mapped(caddr)) {
            int address;

            jclass ia4_class = (*env) -> FindClass(env, "java/net/Inet4Address");
            checkException(env, __LINE__);
            jmethodID ia4_ctrID = (*env) -> GetMethodID(env, ia4_class, "<init>", "()V");
            checkException(env, __LINE__);

            iaObj = (*env) -> NewObject(env, ia4_class, ia4_ctrID);
            checkException(env, __LINE__);
            address = NET_IPv4MappedToIPv4(caddr);
            setInetAddress_addr(env, iaObj, address);
            setInetAddress_family(env, iaObj, IPv4);
        } else {
            jboolean ret;

            jclass ia6_class = (*env) -> FindClass(env, "java/net/Inet6Address");
            checkException(env, __LINE__);
            jmethodID ia6_ctrID = (*env) -> GetMethodID(env, ia6_class, "<init>", "()V");
            checkException(env, __LINE__);

            iaObj = (*env) -> NewObject(env, ia6_class, ia6_ctrID);
            checkException(env, __LINE__);
            ret = setInet6Address_ipaddress(env, iaObj, (char*) & (him6 -> sin6_addr));
            if (ret == JNI_FALSE)
                return NULL;
            setInetAddress_family(env, iaObj, IPv6);
            setInet6Address_scopeid(env, iaObj, him6 -> sin6_scope_id);
        }
        *port = ntohs(him6 -> sin6_port);
    } else {
            struct sockaddr_in *him4 = (struct sockaddr_in*) him;

            jclass ia4_class = (*env) -> FindClass(env, "java/net/Inet4Address");
            checkException(env, __LINE__);
            jmethodID ia4_ctrID = (*env) -> GetMethodID(env, ia4_class, "<init>", "()V");
            checkException(env, __LINE__);

            iaObj = (*env) -> NewObject(env, ia4_class, ia4_ctrID);
            checkException(env, __LINE__);
            setInetAddress_family(env, iaObj, IPv4);
            setInetAddress_addr(env, iaObj, ntohl(him4 -> sin_addr.s_addr));
            *port = ntohs(him4->sin_port);
        }
    return iaObj;
}

//Adapted from JDK
jobject sockAddrToInetSocketAddress(JNIEnv* env, struct sockaddr_storage* sap) {
	int port = 0;

	jobject ia = NET_SockaddrToInetAddress(env, sap, &port);
	checkException(env, __LINE__);

	jclass cls = (*env) -> FindClass(env, "java/net/InetSocketAddress");
	checkException(env, __LINE__);

	jmethodID midInit = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/net/InetAddress;I)V");
	checkException(env, __LINE__);

	return (*env) -> NewObject(env, cls, midInit, ia, port);
}

/**************************************** InetAddress to sockAddr ***********************************/

//Adapted from JDK
int getInetAddress_family(JNIEnv *env, jobject iaObj) {
	jclass c = (*env)->FindClass(env,"java/net/InetAddress");
	checkException(env, __LINE__);
	jclass h = (*env) -> FindClass(env, "java/net/InetAddress$InetAddressHolder");
	checkException(env, __LINE__);
	jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder", "Ljava/net/InetAddress$InetAddressHolder;");
	checkException(env, __LINE__);
	jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
	checkException(env, __LINE__);
	jfieldID iac_familyID = (*env) -> GetFieldID(env, h, "family", "I");
	checkException(env, __LINE__);
    jint family = (*env)->GetIntField(env, holder, iac_familyID);
    checkException(env, __LINE__);
    return family;
}

//Adapted from JDK
int getInetAddress_addr(JNIEnv *env, jobject iaObj) {
	jclass c = (*env)->FindClass(env,"java/net/InetAddress");
	checkException(env, __LINE__);
	jclass h = (*env) -> FindClass(env, "java/net/InetAddress$InetAddressHolder");
	checkException(env, __LINE__);
	jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder", "Ljava/net/InetAddress$InetAddressHolder;");
	checkException(env, __LINE__);
	jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
	checkException(env, __LINE__);
	jfieldID iac_addressID = (*env) -> GetFieldID(env, h, "address", "I");
	checkException(env, __LINE__);
	return (*env)->GetIntField(env, holder, iac_addressID);
}

//Adapted from JDK
jboolean getInet6Address_ipaddress(JNIEnv *env, jobject iaObj, char *dest) {
	jclass c = (*env)->FindClass(env,"java/net/Inet6Address");
	checkException(env, __LINE__);
	if(c == NULL) return JNI_FALSE;
	jclass h = (*env) -> FindClass(env, "java/net/Inet6Address$Inet6AddressHolder");
	checkException(env, __LINE__);
	if(h == NULL) return JNI_FALSE;
	jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder6", "Ljava/net/Inet6Address$Inet6AddressHolder;");
	checkException(env, __LINE__);
	if(ia_holderID == NULL) return JNI_FALSE;
	jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
	checkException(env, __LINE__);
	if(holder == NULL) return JNI_FALSE;
	jfieldID ia6_ipaddressID = (*env)->GetFieldID(env, h, "ipaddress", "[B");
	checkException(env, __LINE__);
	if(ia6_ipaddressID == NULL) return JNI_FALSE;
	jobject addr =  (*env) -> GetObjectField(env, holder, ia6_ipaddressID);
	checkException(env, __LINE__);
    if(addr == NULL) return JNI_FALSE;
    (*env)->GetByteArrayRegion(env, addr, 0, 16, (jbyte *)dest);
    checkException(env, __LINE__);
    return JNI_TRUE;
}

//Adapted from JDK
int getInet6Address_scopeid(JNIEnv *env, jobject iaObj) {
	jclass c = (*env)->FindClass(env,"java/net/Inet6Address");
	checkException(env, __LINE__);
	jclass h = (*env) -> FindClass(env, "java/net/Inet6Address$Inet6AddressHolder");
	checkException(env, __LINE__);
	jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder6", "Ljava/net/Inet6Address$Inet6AddressHolder;");
	checkException(env, __LINE__);
	jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
	checkException(env, __LINE__);
	jfieldID ia6_scopeidID = (*env) -> GetFieldID(env, h, "scope_id", "I");
	checkException(env, __LINE__);
    return (*env)->GetIntField(env, holder, ia6_scopeidID);
}

//Taken from JDK
static int vinit24 = 0;
static int kernelV24 = 0;
int kernelIsV24 () {
    if (!vinit24) {
        struct utsname sysinfo;
        if (uname(&sysinfo) == 0) {
            sysinfo.release[3] = '\0';
            if (strcmp(sysinfo.release, "2.4") == 0) {
                kernelV24 = JNI_TRUE;
            }
        }
        vinit24 = 1;
    }
    return kernelV24;
}

//Taken from JDK
struct loopback_route {
    struct in6_addr addr; /* destination address */
    int plen; /* prefix length */
};

static struct loopback_route *loRoutes = 0;
static int nRoutes = 0; /* number of routes */
static int loRoutes_size = 16; /* initial size */
static int lo_scope_id = 0;
static void initLoopbackRoutes() {
    FILE *f;
    char srcp[8][5];
    char hopp[8][5];
    int dest_plen, src_plen, use, refcnt, metric;
    unsigned long flags;
    char dest_str[40];
    struct in6_addr dest_addr;
    char device[16];
    struct loopback_route *loRoutesTemp;

    if (loRoutes != 0) {
        free (loRoutes);
    }
    loRoutes = calloc (loRoutes_size, sizeof(struct loopback_route));
    if (loRoutes == 0) {
        return;
    }
    /*
     * Scan /proc/net/ipv6_route looking for a matching
     * route.
     */
    if ((f = fopen("/proc/net/ipv6_route", "r")) == NULL) {
        return ;
    }
    while (fscanf(f, "%4s%4s%4s%4s%4s%4s%4s%4s %02x "
                     "%4s%4s%4s%4s%4s%4s%4s%4s %02x "
                     "%4s%4s%4s%4s%4s%4s%4s%4s "
                     "%08x %08x %08x %08lx %8s",
                     dest_str, &dest_str[5], &dest_str[10], &dest_str[15],
                     &dest_str[20], &dest_str[25], &dest_str[30], &dest_str[35],
                     &dest_plen,
                     srcp[0], srcp[1], srcp[2], srcp[3],
                     srcp[4], srcp[5], srcp[6], srcp[7],
                     &src_plen,
                     hopp[0], hopp[1], hopp[2], hopp[3],
                     hopp[4], hopp[5], hopp[6], hopp[7],
                     &metric, &use, &refcnt, &flags, device) == 31) {

        /*
         * Some routes should be ignored
         */
        if ( (dest_plen < 0 || dest_plen > 128)  ||
             (src_plen != 0) ||
             (flags & (RTF_POLICY | RTF_FLOW)) ||
             ((flags & RTF_REJECT) && dest_plen == 0) ) {
            continue;
        }

        /*
         * Convert the destination address
         */
        dest_str[4] = ':';
        dest_str[9] = ':';
        dest_str[14] = ':';
        dest_str[19] = ':';
        dest_str[24] = ':';
        dest_str[29] = ':';
        dest_str[34] = ':';
        dest_str[39] = '\0';

        if (inet_pton(AF_INET6, dest_str, &dest_addr) < 0) {
            /* not an Ipv6 address */
            continue;
        }
        if (strcmp(device, "lo") != 0) {
            /* Not a loopback route */
            continue;
        } else {
            if (nRoutes == loRoutes_size) {
                loRoutesTemp = realloc (loRoutes, loRoutes_size *
                                        sizeof (struct loopback_route) * 2);

                if (loRoutesTemp == 0) {
                    free(loRoutes);
                    fclose (f);
                    return;
                }
                loRoutes=loRoutesTemp;
                loRoutes_size *= 2;
            }
            memcpy (&loRoutes[nRoutes].addr,&dest_addr,sizeof(struct in6_addr));
            loRoutes[nRoutes].plen = dest_plen;
            nRoutes ++;
        }
    }

    fclose (f);
    {
        /* now find the scope_id for "lo" */

        char devname[21];
        char addr6p[8][5];
        int plen, scope, dad_status, if_idx;

        if ((f = fopen("/proc/net/if_inet6", "r")) != NULL) {
            while (fscanf(f, "%4s%4s%4s%4s%4s%4s%4s%4s %08x %02x %02x %02x %20s\n",
                      addr6p[0], addr6p[1], addr6p[2], addr6p[3],
                      addr6p[4], addr6p[5], addr6p[6], addr6p[7],
                  &if_idx, &plen, &scope, &dad_status, devname) == 13) {

                if (strcmp(devname, "lo") == 0) {
                    /*
                     * Found - so just return the index
                     */
                    fclose(f);
                    lo_scope_id = if_idx;
                    return;
                }
            }
            fclose(f);
        }
    }
}

static jboolean needsLoopbackRoute (struct in6_addr* dest_addr) {
    int byte_count;
    int extra_bits, i;
    struct loopback_route *ptr;

    if (loRoutes == 0) {
        initLoopbackRoutes();
    }

    for (ptr = loRoutes, i=0; i<nRoutes; i++, ptr++) {
        struct in6_addr *target_addr=&ptr->addr;
        int dest_plen = ptr->plen;
        byte_count = dest_plen >> 3;
        extra_bits = dest_plen & 0x3;

        if (byte_count > 0) {
            if (memcmp(target_addr, dest_addr, byte_count)) {
                continue;  /* no match */
            }
        }

        if (extra_bits > 0) {
            unsigned char c1 = ((unsigned char *)target_addr)[byte_count];
            unsigned char c2 = ((unsigned char *)&dest_addr)[byte_count];
            unsigned char mask = 0xff << (8 - extra_bits);
            if ((c1 & mask) != (c2 & mask)) {
                continue;
            }
        }
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

//Taken from JDK
int getDefaultIPv6Interface(struct in6_addr *target_addr) {
    FILE *f;
    char srcp[8][5];
    char hopp[8][5];
    int dest_plen, src_plen, use, refcnt, metric;
    unsigned long flags;
    char dest_str[40];
    struct in6_addr dest_addr;
    char device[16];
    jboolean match = JNI_FALSE;

    /*
     * Scan /proc/net/ipv6_route looking for a matching
     * route.
     */
    if ((f = fopen("/proc/net/ipv6_route", "r")) == NULL) {
        return -1;
    }
    while (fscanf(f, "%4s%4s%4s%4s%4s%4s%4s%4s %02x "
                     "%4s%4s%4s%4s%4s%4s%4s%4s %02x "
                     "%4s%4s%4s%4s%4s%4s%4s%4s "
                     "%08x %08x %08x %08lx %8s",
                     dest_str, &dest_str[5], &dest_str[10], &dest_str[15],
                     &dest_str[20], &dest_str[25], &dest_str[30], &dest_str[35],
                     &dest_plen,
                     srcp[0], srcp[1], srcp[2], srcp[3],
                     srcp[4], srcp[5], srcp[6], srcp[7],
                     &src_plen,
                     hopp[0], hopp[1], hopp[2], hopp[3],
                     hopp[4], hopp[5], hopp[6], hopp[7],
                     &metric, &use, &refcnt, &flags, device) == 31) {

        /*
         * Some routes should be ignored
         */
        if ( (dest_plen < 0 || dest_plen > 128)  ||
             (src_plen != 0) ||
             (flags & (RTF_POLICY | RTF_FLOW)) ||
             ((flags & RTF_REJECT) && dest_plen == 0) ) {
            continue;
        }

        /*
         * Convert the destination address
         */
        dest_str[4] = ':';
        dest_str[9] = ':';
        dest_str[14] = ':';
        dest_str[19] = ':';
        dest_str[24] = ':';
        dest_str[29] = ':';
        dest_str[34] = ':';
        dest_str[39] = '\0';

        if (inet_pton(AF_INET6, dest_str, &dest_addr) < 0) {
            /* not an Ipv6 address */
            continue;
        } else {
            /*
             * The prefix len (dest_plen) indicates the number of bits we
             * need to match on.
             *
             * dest_plen / 8    => number of bytes to match
             * dest_plen % 8    => number of additional bits to match
             *
             * eg: fe80::/10 => match 1 byte + 2 additional bits in the
             *                  the next byte.
             */
            int byte_count = dest_plen >> 3;
            int extra_bits = dest_plen & 0x3;

            if (byte_count > 0) {
                if (memcmp(target_addr, &dest_addr, byte_count)) {
                    continue;  /* no match */
                }
            }

            if (extra_bits > 0) {
                unsigned char c1 = ((unsigned char *)target_addr)[byte_count];
                unsigned char c2 = ((unsigned char *)&dest_addr)[byte_count];
                unsigned char mask = 0xff << (8 - extra_bits);
                if ((c1 & mask) != (c2 & mask)) {
                    continue;
                }
            }

            /*
             * We have a match
             */
            match = JNI_TRUE;
            break;
        }
    }
    fclose(f);

    /*
     * If there's a match then we lookup the interface
     * index.
     */
    if (match) {
        char devname[21];
        char addr6p[8][5];
        int plen, scope, dad_status, if_idx;

        if ((f = fopen("/proc/net/if_inet6", "r")) != NULL) {
            while (fscanf(f, "%4s%4s%4s%4s%4s%4s%4s%4s %08x %02x %02x %02x %20s\n",
                      addr6p[0], addr6p[1], addr6p[2], addr6p[3],
                      addr6p[4], addr6p[5], addr6p[6], addr6p[7],
                  &if_idx, &plen, &scope, &dad_status, devname) == 13) {

                if (strcmp(devname, device) == 0) {
                    /*
                     * Found - so just return the index
                     */
                    fclose(f);
                    return if_idx;
                }
            }
            fclose(f);
        } else {
            /*
             * Couldn't open /proc/net/if_inet6
             */
            return -1;
        }
    }

    /*
     * If we get here it means we didn't there wasn't any
     * route or we couldn't get the index of the interface.
     */
    return 0;
}

//Taken from JDK
struct localinterface {
    int index;
    char localaddr [16];
};

static struct localinterface *localifs = 0;
static int localifsSize = 0;    /* size of array */
static int nifs = 0;            /* number of entries used in array */

static void initLocalIfs () {
    FILE *f;
    unsigned char staddr [16];
    char ifname [33];
    struct localinterface *lif=0;
    int index, x1, x2, x3;
    unsigned int u0,u1,u2,u3,u4,u5,u6,u7,u8,u9,ua,ub,uc,ud,ue,uf;

    if ((f = fopen("/proc/net/if_inet6", "r")) == NULL) {
        return ;
    }
    while (fscanf (f, "%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x%2x "
                "%d %x %x %x %32s",&u0,&u1,&u2,&u3,&u4,&u5,&u6,&u7,
                &u8,&u9,&ua,&ub,&uc,&ud,&ue,&uf,
                &index, &x1, &x2, &x3, ifname) == 21) {
        staddr[0] = (unsigned char)u0;
        staddr[1] = (unsigned char)u1;
        staddr[2] = (unsigned char)u2;
        staddr[3] = (unsigned char)u3;
        staddr[4] = (unsigned char)u4;
        staddr[5] = (unsigned char)u5;
        staddr[6] = (unsigned char)u6;
        staddr[7] = (unsigned char)u7;
        staddr[8] = (unsigned char)u8;
        staddr[9] = (unsigned char)u9;
        staddr[10] = (unsigned char)ua;
        staddr[11] = (unsigned char)ub;
        staddr[12] = (unsigned char)uc;
        staddr[13] = (unsigned char)ud;
        staddr[14] = (unsigned char)ue;
        staddr[15] = (unsigned char)uf;
        nifs ++;
        if (nifs > localifsSize) {
            localifs = (struct localinterface *) realloc (
                        localifs, sizeof (struct localinterface)* (localifsSize+5));
            if (localifs == 0) {
                nifs = 0;
                fclose (f);
                return;
            }
            lif = localifs + localifsSize;
            localifsSize += 5;
        } else {
            lif ++;
        }
        memcpy (lif->localaddr, staddr, 16);
        lif->index = index;
    }
    fclose (f);
}

static int getLocalScopeID (char *addr) {
    struct localinterface *lif;
    int i;
    if (localifs == 0) {
        initLocalIfs();
    }
    for (i=0, lif=localifs; i<nifs; i++, lif++) {
        if (memcmp (addr, lif->localaddr, 16) == 0) {
            return lif->index;
        }
    }
    return 0;
}

//Adapted from JDK
int NET_InetAddressToSockaddr(JNIEnv *env, jobject iaObj, int port, struct sockaddr_storage *him, int *len, jboolean v4MappedAddress) {
    jint family = getInetAddress_family(env, iaObj);
    if (IPv6_supported() && !(family == IPv4 && v4MappedAddress == JNI_FALSE)) {
        struct sockaddr_in6* him6 = (struct sockaddr_in6*) him;
        jbyte caddr[16];
        jint address;

        if (family == IPv4) {
            memset((char *) caddr, 0, 16);
            address = getInetAddress_addr(env, iaObj);
            if(address != INADDR_ANY) {
                caddr[10] = 0xff;
                caddr[11] = 0xff;
                caddr[12] = ((address >> 24) & 0xff);
                caddr[13] = ((address >> 16) & 0xff);
                caddr[14] = ((address >> 8) & 0xff);
                caddr[15] = (address & 0xff);
            }
        } else {
            getInet6Address_ipaddress(env, iaObj, (char*) caddr);
        }

        memset((char*) him6, 0, sizeof(struct sockaddr_in6));
        him6->sin6_port = htons(port);
        memcpy((void*) &(him6 -> sin6_addr), caddr, sizeof(struct in6_addr) );
        him6->sin6_family = AF_INET6;
        *len = sizeof(struct sockaddr_in6) ;

        jclass ia6_class = (*env)->FindClass(env,"java/net/Inet6Address");
        checkException(env, __LINE__);
        if (IN6_IS_ADDR_LINKLOCAL(&(him6->sin6_addr))) {
            int cached_scope_id = 0, scope_id = 0;

            jfieldID ia6_cachedscopeidID = (*env)->GetFieldID(env, ia6_class, "cached_scope_id", "I");
            checkException(env, __LINE__);
            if (ia6_cachedscopeidID) {
                cached_scope_id = (int)(*env)->GetIntField(env, iaObj, ia6_cachedscopeidID);
                checkException(env, __LINE__);
                /* if cached value exists then use it. Otherwise, check
                 * if scope is set in the address.
                 */
                if (!cached_scope_id) {
                	jclass ia6h_class = (*env)->FindClass(env, "java/net/Inet6Address$Inet6AddressHolder");
                	checkException(env, __LINE__);
                	jfieldID ia6_scopeidID = (*env)->GetFieldID(env, ia6h_class, "scope_id", "I");
                	checkException(env, __LINE__);
                    if (ia6_scopeidID) {
                        scope_id = getInet6Address_scopeid(env, iaObj);
                    }
                    if (scope_id != 0) {
                        /* check user-specified value for loopback case
                         * that needs to be overridden
                         */
                        if (kernelIsV24() && needsLoopbackRoute (&him6->sin6_addr)) {
                            cached_scope_id = lo_scope_id;
                            (*env)->SetIntField(env, iaObj, ia6_cachedscopeidID, cached_scope_id);
                            checkException(env, __LINE__);
                        }
                    } else {
                        /*
                         * Otherwise consult the IPv6 routing tables to
                         * try determine the appropriate interface.
                         */
                        if (kernelIsV24()) {
                            cached_scope_id = getDefaultIPv6Interface( &(him6->sin6_addr) );
                        } else {
                            cached_scope_id = getLocalScopeID( (char *)&(him6->sin6_addr) );
                            if (cached_scope_id == 0) {
                                cached_scope_id = getDefaultIPv6Interface( &(him6->sin6_addr) );
                            }
                        }
                        (*env)->SetIntField(env, iaObj, ia6_cachedscopeidID, cached_scope_id);
                        checkException(env, __LINE__);
                    }
                }
            }

            /*
             * If we have a scope_id use the extended form
             * of sockaddr_in6.
             */

            struct sockaddr_in6 *him6 =
                    (struct sockaddr_in6 *)him;
            him6->sin6_scope_id = cached_scope_id != 0 ?
                                        cached_scope_id    : scope_id;
            *len = sizeof(struct sockaddr_in6);
        }
    } else
        {
            struct sockaddr_in *him4 = (struct sockaddr_in*)him;
            jint address;
            if (family == IPv6) {
              return -1;
            }
            memset((char *) him4, 0, sizeof(struct sockaddr_in));
            address = getInetAddress_addr(env, iaObj);
            him4->sin_port = htons((short) port);
            him4->sin_addr.s_addr = (uint32_t) htonl(address);
            him4->sin_family = AF_INET;
            *len = sizeof(struct sockaddr_in);
        }
    return 0;
}

struct sockaddr_storage InetSocketAddressToSockAddr(JNIEnv *env, jobject iaObj, jboolean v4MappedAddress) {
	struct sockaddr_storage sap;
    int sap_len = sizeof(sap);

    jclass c = (*env)->FindClass(env,"java/net/InetSocketAddress");
    checkException(env, __LINE__);
    jclass h = (*env) -> FindClass(env, "java/net/InetSocketAddress$InetSocketAddressHolder");
    checkException(env, __LINE__);
    jfieldID ia_holderID = (*env) -> GetFieldID(env, c, "holder", "Ljava/net/InetSocketAddress$InetSocketAddressHolder;");
    checkException(env, __LINE__);
    jobject holder = (*env) -> GetObjectField(env, iaObj, ia_holderID);
    checkException(env, __LINE__);

    jfieldID addrID = (*env) -> GetFieldID(env, h, "addr", "Ljava/net/InetAddress;");
    checkException(env, __LINE__);
    jobject addr = (*env)->GetObjectField(env, holder, addrID);
    checkException(env, __LINE__);

    jfieldID field = (*env) -> GetFieldID(env, h, "port", "I");
    checkException(env, __LINE__);
    jint port = (*env) -> GetIntField(env, holder, field);
    checkException(env, __LINE__);
    
    NET_InetAddressToSockaddr(env, addr, port, &sap, &sap_len, v4MappedAddress);
    return sap;
}
