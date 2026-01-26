package id.xms.xtrakernelmanager.utils

object ProcConstants {
    // TCP congestion control paths
    const val TCP_CONGESTION_ALGORITHM = "/proc/sys/net/ipv4/tcp_congestion_control"
    const val TCP_AVAILABLE_CONGESTION_ALGORITHM = "/proc/sys/net/ipv4/tcp_available_congestion_control"
    
    // Other proc filesystem paths can be added here
}