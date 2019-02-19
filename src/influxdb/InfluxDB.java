package influxdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class InfluxDB {
	private static final String CONST_CONTENT_LENGTH = "Content-Length:";
	private static final String CONST_TRANSFER_ENCODING = "Transfer-Encoding:";
	private static final String CONST_HTTP_HEAD = "HTTP";
	private static final String CONST_HTTP_CODE_200 = "200";

	private static final int CONST_CODE_BAD_HTTP_HEADER = -1;//没有收到正确的HTTP头
	private static final int CONST_CODE_NOT_HTTP_200 = -2;//收到了正确的HTTP头，但是不是HTTP200
	private static final int CONST_CODE_CANNT_FIND_CONTENT_LENGTH = -3;//没有找到Content-Length

	private static final int CONST_CODE_PARSE_JSON_FAILED = -4;//解析Json失败

	private static final int CONST_CODE_PARSE_HTTP_CHUNKED_ERROR = -5;//解析chunked编码失败

	private static final int CONST_STATUS_CONNECTION_FIALED = -10;//网络连接异常

	private static final int CONST_HTTP_TRANSFER_ENCODING_NORMAL = 0;//普通的http正文
	private static final int CONST_HTTP_TRANSFER_ENCODING_CHUNKED = 1;//chunked的http正文

	private String serverIp;
	private int serverPort;
	private Socket socket;
	private String lastError;

	private char[] httpBodyReceiveBuffer = new char[1024];
	private int httpBodyReceiveBufferLength;

	private int transferEncodingType = CONST_HTTP_TRANSFER_ENCODING_NORMAL;
	private boolean jsonParseSuccess;
	private JsonObject anaylsisResult;

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public Boolean CheckServerConfig() {
		if (serverIp.trim().isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

	public String getLastError() {
		return lastError;
	}

	private void setLastError(String lastError, boolean doPrint) {
		this.lastError = lastError;
		if (doPrint) {
			System.err.println(lastError);
		}
	}

	public Boolean Connect() {
		try {
			socket = new Socket(getServerIp(), getServerPort());
			socket.setKeepAlive(true);
			socket.setSoTimeout(30000);
			return true;
		} catch (Exception e) {
			setLastError(e.getMessage(), true);
			e.printStackTrace();
			return false;
		}
	}

	public Boolean DisConnect() {
		try {
			socket.shutdownInput();
			socket.shutdownOutput();
			socket.close();
			return true;
		} catch (Exception e) {
			setLastError(e.getMessage(), true);
			return false;
		}
	}

	private OutputStream GetOutputStream() {
		if (socket != null) {
			try {
				OutputStream outputStream = socket.getOutputStream();
				return outputStream;
			} catch (Exception e) {
				setLastError(e.getMessage(), true);
				return null;
			}
		} else {
			setLastError("socket can't be null.'", true);
			return null;
		}
	}

	private BufferedReader GetInputStream() {
		if (socket != null) {
			try {
				InputStream inputStream = socket.getInputStream();
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
				return bufferedReader;
			} catch (Exception e) {
				setLastError(e.getMessage(), true);
				return null;
			}
		} else {
			setLastError("socket cant't be null.", true);
			return null;
		}
	}

	private boolean SendGet(String getPath) {
		OutputStream outputStream = GetOutputStream();
		try {
			if (outputStream != null) {
				StringBuilder headers = new StringBuilder();
				headers.append("GET ");
				headers.append(getPath);
				headers.append(" HTTP/1.1\r\n");
				headers.append("User-Agent: Java/1.6.0_20\r\n");
				headers.append("Host: ");
				headers.append(getServerIp());
				headers.append(":");
				headers.append((getServerPort() + "\r\n"));
				headers.append("Accept: */*\r\n");
				headers.append("Connection: keep-alive\r\n");
				headers.append("Keep-Alive: 30000\r\n");
				headers.append("\r\n");
				// System.out.println(headers.toString());
				outputStream.write(headers.toString().getBytes());
				return true;
			}
		} catch (Exception e) {
			setLastError(e.getMessage(), true);
		}
		return false;
	}

	public InfluxDataAdapter Query(String dbName, String sql) {
		StringBuilder getPath = new StringBuilder();
		getPath.append("/query?");
		getPath.append("db=");
		getPath.append(dbName);
		getPath.append("&q=");
		getPath.append(UrlEncoder.EncodeString(sql));

		// System.out.println(getPath);
		if (SendGet(getPath.toString())) {
			int resultCode = AnaylsisResponse();
			if (resultCode == 0) {
				InfluxDataAdapter adapter = new InfluxDataAdapter(anaylsisResult);
				return adapter;
			}
		}
		return null;
	}

	private int AnaylsisResponse() {
		BufferedReader bufferedReader = GetInputStream();
		if (bufferedReader != null) {
			//先解析HTTP头部
			int resultCode = AnaylsisHttpHeaders(bufferedReader);
			if (resultCode > 0) {
				//头部正常，并且有Content-Length
				int contentLength = resultCode;
				//开始解析Http正文部分
				resultCode = AnaylsisHttpBody(bufferedReader, contentLength);
				if (resultCode != 0) {
					return resultCode;
				}
			} else if (transferEncodingType == CONST_HTTP_TRANSFER_ENCODING_CHUNKED) {
				resultCode = AnaylsisHttpBody(bufferedReader);
			} else {
				return resultCode;
			}
		}
		return 0;
	}

	/**
	 * 解析HTTP头部
	 * @param inputStream HTTP输入流
	 * @return 当返回值大于0时，返回值为Content-Length；当返回值小于0时返回对应的CODE常量
	 */
	private int AnaylsisHttpHeaders(BufferedReader bufferedReader) {
		try {
			boolean httpHeadWellFormed = true;
			boolean httpHeadOK = true;
			transferEncodingType = CONST_HTTP_TRANSFER_ENCODING_NORMAL;
			//服务器响应的第一行为HTTP请求结果状态
			String httpCodeLine = bufferedReader.readLine();
			// System.out.println(httpCodeLine);
			String[] httpCodeLineParts = httpCodeLine.split(" ");
			//正常情况下为 HTTP/1.1 200 OK
			if (httpCodeLineParts == null || httpCodeLineParts.length != 3) {
				setLastError("Bad Http Header:\r\n" + httpCodeLine, true);
				httpHeadWellFormed = false;
			} else {
				if ((!httpCodeLineParts[0].contains(CONST_HTTP_HEAD))
						|| (!httpCodeLineParts[1].contains(CONST_HTTP_CODE_200))) {
					//说明服务端返回了http错误码
					setLastError("Http Header is not OK:\r\n" + httpCodeLine, true);
					httpHeadOK = false;
				}
			}
			//开始查找Http头的结尾
			//是否继续寻找http头结尾的标记
			boolean keepFoundHttpEnd = true;
			int contentLength = -1;
			//开始循环查找HTTP头结尾
			while (keepFoundHttpEnd) {
				String line = bufferedReader.readLine();
				// System.out.println(line);
				if (line == null) {
					// System.out.println("Reached the end of stream");
					break;
				}
				// System.out.println(line);
				if (line.isEmpty()) {
					// System.out.println("Reached the end of http headers");
					keepFoundHttpEnd = false;
				} else {
					//对http头部做解析
					//找到Content-Length
					if (line.contains(CONST_CONTENT_LENGTH) && line.length() > CONST_CONTENT_LENGTH.length()) {
						String lengthStr = line.substring(CONST_CONTENT_LENGTH.length() + 1).trim();
						if (!lengthStr.isEmpty()) {
							contentLength = Integer.parseInt(lengthStr);
							// System.out.println("Content-Length is " + contentLength);
						}
					} else if (line.contains(CONST_TRANSFER_ENCODING)
							&& line.length() > CONST_TRANSFER_ENCODING.length()) {
						//如果找到Transfer-Encoding，则说明是chunked模式
						transferEncodingType = CONST_HTTP_TRANSFER_ENCODING_CHUNKED;
					}
				}
			}
			if (!httpHeadWellFormed) {
				//没有收到正确的HTTP头
				return CONST_CODE_BAD_HTTP_HEADER;
			}
			if (!httpHeadOK) {
				//收到了正确的HTTP头，但是不是HTTP200
				return CONST_CODE_NOT_HTTP_200;
			}
			if (keepFoundHttpEnd) {
				//没有找到Content-Length
				return CONST_CODE_CANNT_FIND_CONTENT_LENGTH;
			}
			return contentLength;
		} catch (IOException e) {
			setLastError(e.getMessage(), true);
			//网络连接异常
			return CONST_STATUS_CONNECTION_FIALED;
		}
	}

	private int AnaylsisHttpBody(BufferedReader bufferedReader, int contentLength) {
		//解析结果存放在anaylsisResult中
		anaylsisResult = null;
		//接收到的总数据的长度
		int receivedDataLenght = 0;
		try {
			//开始进行Json解析
			JsonDeserializer jsonDeserializer = new JsonDeserializer();
			jsonParseSuccess = true;//标记Json解析过程中是否发生过错误
			while (receivedDataLenght < contentLength) {
				int onceReceiveLength;
				int remainLength = contentLength - receivedDataLenght;//尚未接收的内容大小
				if (remainLength > httpBodyReceiveBuffer.length) {
					//如果尚未接收的内容超出缓冲区大小，则一次性接收缓冲区大小的内容
					onceReceiveLength = httpBodyReceiveBuffer.length;
				} else {
					//如果尚未接收的内容小于或等于缓冲区大小，则一次性接收全部剩余内容
					onceReceiveLength = remainLength;
				}

				httpBodyReceiveBufferLength = bufferedReader.read(httpBodyReceiveBuffer, 0, onceReceiveLength);//从缓冲区取出一些数据
				receivedDataLenght += httpBodyReceiveBufferLength;//更新已读取的总长度

				//如果解析过程中发送过错误，则放弃解析，并读取到HttpBody结尾
				if (jsonParseSuccess) {
					//将缓冲区的内容送给JsonDeserializer解析
					int result = jsonDeserializer.InputData(httpBodyReceiveBuffer, httpBodyReceiveBufferLength);
					if (result != 0) {
						jsonParseSuccess = false;
					}
				}
			}
			if (jsonParseSuccess) {
				//读取HttpBody结束，并且解析过程中未发生错误，停止解析，并获取解析结果
				anaylsisResult = jsonDeserializer.EndInput();
			} else {
				//解析Json失败
				setLastError(jsonDeserializer.getLastError(), true);
				return CONST_CODE_PARSE_JSON_FAILED;
			}

			return 0;
		} catch (IOException e) {
			setLastError(e.getMessage(), true);
			//网络连接异常
			return CONST_STATUS_CONNECTION_FIALED;
		}
	}

	private int AnaylsisHttpBody(BufferedReader bufferedReader) {
		anaylsisResult = null;
		JsonDeserializer jsonDeserializer = new JsonDeserializer();
		int chunkSize = ReceiveChunkedPart(bufferedReader, jsonDeserializer);
		while (chunkSize > 0) {
			chunkSize = ReceiveChunkedPart(bufferedReader, jsonDeserializer);
		}
		if (jsonParseSuccess) {
			return 0;
		} else {
			return -5;
		}
	}

	private int ReceiveChunkedPart(BufferedReader bufferedReader, JsonDeserializer jsonDeserializer) {
		try {
			String chunkSizeStr = bufferedReader.readLine();
			// System.out.println(chunkSizeStr);
			int chunkSize = Integer.parseInt(chunkSizeStr, 16);
			// System.out.println("chunk size: " + chunkSize);

			//接收到的总数据的长度
			int receivedDataLenght = 0;
			//开始进行Json解析
			jsonParseSuccess = true;//标记Json解析过程中是否发生过错误
			while (receivedDataLenght < chunkSize) {
				int onceReceiveLength;
				int remainLength = chunkSize - receivedDataLenght;//尚未接收的内容大小
				if (remainLength > httpBodyReceiveBuffer.length) {
					//如果尚未接收的内容超出缓冲区大小，则一次性接收缓冲区大小的内容
					onceReceiveLength = httpBodyReceiveBuffer.length;
				} else {
					//如果尚未接收的内容小于或等于缓冲区大小，则一次性接收全部剩余内容
					onceReceiveLength = remainLength;
				}

				httpBodyReceiveBufferLength = bufferedReader.read(httpBodyReceiveBuffer, 0, onceReceiveLength);//从缓冲区取出一些数据
				receivedDataLenght += httpBodyReceiveBufferLength;//更新已读取的总长度

				// System.out.println(httpBodyReceiveBuffer);

				//如果解析过程中发送过错误，则放弃解析，并读取到HttpBody结尾
				if (jsonParseSuccess) {
					//将缓冲区的内容送给JsonDeserializer解析
					int result = jsonDeserializer.InputData(httpBodyReceiveBuffer, httpBodyReceiveBufferLength);
					if (result != 0) {
						jsonParseSuccess = false;
					}
				}
			}
			// System.out.println(bufferedReader.readLine());
			bufferedReader.readLine();
			if (chunkSize == 0) {
				anaylsisResult = jsonDeserializer.EndInput();
			}
			return chunkSize;
		} catch (IOException e) {
			setLastError(e.getMessage(), true);
			//网络连接异常
			return CONST_STATUS_CONNECTION_FIALED;
		}
	}
}
