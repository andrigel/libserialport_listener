package com.omega.libserialport_listener

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import android.util.Log
import androidx.annotation.IntDef
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.io.FileReader
import java.io.LineNumberReader
import java.util.Iterator
import java.util.Vector

class LibserialportListenerPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel : MethodChannel

    protected lateinit var mSerialPort: SerialPort
    protected lateinit var mOutputStream: OutputStream
    private lateinit var mInputStream: InputStream
    private lateinit var mReadThread: ReadThread
    val mSerialPortFinder = SerialPortFinder()
    var isInitialized: Boolean = false

    private class ReadThread(val mInputStream: InputStream,private val listener: (String) -> Unit) : Thread() {
    override fun run() {
        super.run()
        while (!isInterrupted()) {
            var size: Int
            try {
                val buffer = ByteArray(64)
                if (mInputStream == null) return
                size = mInputStream.read(buffer)
                if (size > 0) {
                  listener(String(buffer, 0, size))
                }
            } catch (e: IOException) {
                e.printStackTrace()
                    listener(e.toString())
                return
            }
        }
    }
  }

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "libserialport_listener")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "start_listener") {
        val path:String? = call.argument("device_path");
        if(path != null)
        {
            getConnect(path)
        }
    runOnUiThread {
      channel.invokeMethod("call_native",path)
        }
      result.success("")
    } else if (call.method == "get_devices_path") {
      result.success(mSerialPortFinder.getAllDevicesPath().asList())
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    mReadThread.interrupt()
    channel.setMethodCallHandler(null)
  }

  fun onDataReceived(result: String){
    Log.i("CustomLog",result)
    runOnUiThread {
    channel?.invokeMethod("port_value_change",result)
    }
  }

  fun getConnect(path: String) {
    if (!isInitialized) {
        try {
            mSerialPort = SerialPort(File(path), 9600)
          
            mOutputStream = mSerialPort.getOutputStream()
            mInputStream = mSerialPort.getInputStream()
            mReadThread = ReadThread(mInputStream,{res -> onDataReceived(res)})
            mReadThread.start()
            isInitialized = true
        } 
        catch (e: Throwable) {
            onDataReceived(e.toString())
        } 
    }
  }
}


class SerialPort {
    private val TAG = "SerialPort"
    private var sSuPath = "/system/bin/su"
    private lateinit var mFd: FileDescriptor
    private lateinit var mFileInputStream: FileInputStream
    private lateinit var mFileOutputStream: FileOutputStream

    @IntDef(value = [Parity.NONE, Parity.ODD, Parity.EVEN])
    @Retention(RetentionPolicy.SOURCE)
    annotation class Parity {
        companion object {
            const val NONE = 0
            const val ODD = 1
            const val EVEN = 2
        }
    }

    @IntDef(value = [DataBit.B5, DataBit.B6, DataBit.B7, DataBit.B8])
    @Retention(RetentionPolicy.SOURCE)
    annotation class DataBit {
        companion object {
            const val B5 = 5
            const val B6 = 6
            const val B7 = 7
            const val B8 = 8
        }
    }

    @IntDef(value = [StopBit.B1, StopBit.B2])
    @Retention(RetentionPolicy.SOURCE)
    annotation class StopBit {
        companion object {
            const val B1 = 1
            const val B2 = 2
        }
    }

    fun setSuPath(suPath: String?) {
        if (suPath == null) {
            // TODO: Handle null suPath
        } else {
            sSuPath = suPath
        }
    }
    fun getSutPath(): String {
      return sSuPath;
    }

    @Throws(IOException::class)
    constructor(device: File, baudRate: Int) : this(device, baudRate, 0)

    @Throws(IOException::class)
    constructor(file: File, baudRate: Int, flags: Int) : this(file, baudRate, Parity.NONE, DataBit.B8, StopBit.B1, flags)

    @Throws(IOException::class)
    constructor(file: File, baudRate: Int, parity: Int, dataBits: Int, stopBit: Int) : this(file, baudRate, parity, dataBits, stopBit, 0)
    
    constructor(device: File, baudRate: Int, parity: Int, dataBits: Int, stopBit: Int, flags: Int) {
        Log.i(TAG, "init SerialPort")
        System.loadLibrary("serial-port")
        if (!device.canRead() || !device.canWrite()) {
            Log.i(TAG, "Missing read/write permission, trying to chmod the file")
            try {
                val su: Process
                su = Runtime.getRuntime().exec(sSuPath)
                val cmd = "chmod 666 " + device.absolutePath + "\n" + "exit\n"
                su.outputStream.write(cmd.toByteArray())
                if ((su.waitFor() != 0) || !device.canRead() || !device.canWrite()) {
                    throw IOException("open serial port failure")
                }
            } catch (e: Exception) {
                throw IOException("open serial port failure", e)
            }
        }
        mFd = open(device.absolutePath, baudRate, parity, dataBits, stopBit, flags)
        if (mFd == null) {
            Log.e(TAG, "native open returns null")
            throw IOException("open serial port failure")
        }
        mFileInputStream = FileInputStream(mFd)
        mFileOutputStream = FileOutputStream(mFd)
        Log.i(TAG, "SerialPort initialized")
    }
    
    fun getInputStream(): InputStream {
        return mFileInputStream
    }
    fun getOutputStream(): OutputStream {
        return mFileOutputStream
    }

    private external fun open(
    path: String,
    baudRate: Int,
    parity: Int,
    dataBits: Int,
    stopBit: Int,
    flags: Int
    ): FileDescriptor
}



class SerialPortFinder {
    inner class Driver(name: String, root: String) {
        private val mDriverName: String = name
        private val mDeviceRoot: String = root
        private var mDevices: Vector<File>? = null

        fun getDevices(): Vector<File> {
            if (mDevices == null) {
                mDevices = Vector<File>()
                val dev = File("/dev")
                val files = dev.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.absolutePath.startsWith(mDeviceRoot)) {
                            Log.i(TAG, "Found new device: $file")
                            mDevices!!.add(file)
                        }
                    }
                }
            }
            return mDevices!!
        }

        fun getName(): String {
            return mDriverName
        }
    }

    companion object {
        private const val TAG = "SerialPort"
    }

    private lateinit var mDrivers: Vector<Driver>

    @Throws(IOException::class)
    private fun getDrivers(): Vector<Driver> {
//    if (mDrivers == null) {
    mDrivers = Vector<Driver>()
    val r = LineNumberReader(FileReader("/proc/tty/drivers"))
    var l: String?
    while (r.readLine().also { l = it } != null) {
        val drivername = l!!.substring(0, 0x15).trim()
        val w = l!!.split(" +".toRegex()).toTypedArray()
        if (w.size >= 5 && w[w.size - 1] == "serial") {
            Log.d(TAG, "Found new driver $drivername on ${w[w.size - 4]}")
            mDrivers.add(Driver(drivername, w[w.size - 4]))
        }
    }
    r.close()
// }
return mDrivers
    }

    fun getAllDevices(): Array<String> {
    val devices = mutableListOf<String>()
    
    val itdriv = getDrivers().iterator()
    try {
        while (itdriv.hasNext()) {
            val driver = itdriv.next()
            val itdev = driver.getDevices().iterator()
            while (itdev.hasNext()) {
                val device = itdev.next().name
                val value = "$device (${driver.getName()})"
                devices.add(value)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return devices.toTypedArray()
}

fun getAllDevicesPath(): Array<String> {
    val devices = mutableListOf<String>()
    
    val itdriv = getDrivers().iterator()
    try {
        while (itdriv.hasNext()) {
            val driver = itdriv.next()
            val itdev = driver.getDevices().iterator()
            while (itdev.hasNext()) {
                val device = itdev.next().absolutePath
                devices.add(device)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return devices.toTypedArray()
}
}
