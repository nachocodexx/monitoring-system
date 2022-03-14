package mx.cinvestav.config

import mx.cinvestav.config
case class DefaultConfig(
                        nodeId:String,
                        poolId:String,
                        host:String,
                        port:Int,
                        apiVersion:Int,
                        delayMs:Long,
                        bufferSize:Int,
                        monitoringEnabled:Boolean
                        )
