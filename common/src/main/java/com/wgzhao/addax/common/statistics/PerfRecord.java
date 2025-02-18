/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wgzhao.addax.common.statistics;

import com.wgzhao.addax.common.util.HostUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

/**
 * Created by liqiang on 15/8/23.
 */
@SuppressWarnings("NullableProblems")
public class PerfRecord
        implements Comparable<PerfRecord>
{
    private static final Logger perf = LoggerFactory.getLogger(PerfRecord.class);
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final int taskGroupId;
    private final int taskId;
    private final PHASE phase;
    private volatile ACTION action;
    private volatile Date startTime; //NOSONAR
    private volatile long elapsedTimeInNs = -1;
    private volatile long count = 0;
    private volatile long size = 0;
    private volatile long startTimeInNs;
    private volatile boolean isReport = false;

    public PerfRecord(int taskGroupId, int taskId, PHASE phase)
    {
        this.taskGroupId = taskGroupId;
        this.taskId = taskId;
        this.phase = phase;
    }

    public static void addPerfRecord(int taskGroupId, int taskId, PHASE phase, long startTime, long elapsedTimeInNs)
    {
        if (PerfTrace.getInstance().isEnable()) {
            PerfRecord perfRecord = new PerfRecord(taskGroupId, taskId, phase);
            perfRecord.elapsedTimeInNs = elapsedTimeInNs;
            perfRecord.action = ACTION.END;
            perfRecord.startTime = new Date(startTime);
            //在PerfTrace里注册
            PerfTrace.getInstance().tracePerfRecord(perfRecord);
            perf.info(perfRecord.toString());
        }
    }

    public void start()
    {
        if (PerfTrace.getInstance().isEnable()) {
            this.startTime = new Date();
            this.startTimeInNs = System.nanoTime();
            this.action = ACTION.START;
            //在PerfTrace里注册
            PerfTrace.getInstance().tracePerfRecord(this);
            perf.info(toString());
        }
    }

    public void addCount(long count)
    {
        this.count += count;
    }

    public void addSize(long size)
    {
        this.size += size;
    }

    public void end()
    {
        if (PerfTrace.getInstance().isEnable()) {
            this.elapsedTimeInNs = System.nanoTime() - startTimeInNs;
            this.action = ACTION.END;
            PerfTrace.getInstance().tracePerfRecord(this);
            perf.info(toString());
        }
    }

    public void end(long elapsedTimeInNs)
    {
        if (PerfTrace.getInstance().isEnable()) {
            this.elapsedTimeInNs = elapsedTimeInNs;
            this.action = ACTION.END;
            PerfTrace.getInstance().tracePerfRecord(this);
            perf.info(toString());
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s"
                , getInstId(), taskGroupId, taskId, phase, action,
                DateFormatUtils.format(startTime, DATETIME_FORMAT), elapsedTimeInNs, count, size, getHostIP());
    }

    @Override
    public int compareTo(PerfRecord o)
    {
        if (o == null) {
            return 1;
        }
        return Long.compare(this.elapsedTimeInNs, o.elapsedTimeInNs);
    }

    @Override
    public int hashCode()
    {
        long jobId = getInstId();
        int result = (int) (jobId ^ (jobId >>> 32));
        result = 31 * result + taskGroupId;
        result = 31 * result + taskId;
        result = 31 * result + phase.toInt();
        result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PerfRecord)) {
            return false;
        }

        PerfRecord dst = (PerfRecord) o;

        if (this.getInstId() != dst.getInstId()) {
            return false;
        }
        if (this.taskGroupId != dst.taskGroupId) {
            return false;
        }
        if (this.taskId != dst.taskId) {
            return false;
        }
        if (!Objects.equals(phase, dst.phase)) {
            return false;
        }
        return Objects.equals(startTime, dst.startTime);
    }

    public int getTaskGroupId()
    {
        return taskGroupId;
    }

    public int getTaskId()
    {
        return taskId;
    }

    public PHASE getPhase()
    {
        return phase;
    }

    public ACTION getAction()
    {
        return action;
    }

    public long getElapsedTimeInNs()
    {
        return elapsedTimeInNs;
    }

    public long getCount()
    {
        return count;
    }

    public long getSize()
    {
        return size;
    }

    public long getInstId()
    {
        return PerfTrace.getInstance().getInstId();
    }

    public String getHostIP()
    {
        return HostUtils.IP;
    }

    public String getHostName()
    {
        return HostUtils.HOSTNAME;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public long getStartTimeInMs()
    {
        return startTime.getTime();
    }

    public long getStartTimeInNs()
    {
        return startTimeInNs;
    }

    public String getDatetime()
    {
        if (startTime == null) {
            return "null time";
        }
        return DateFormatUtils.format(startTime, DATETIME_FORMAT);
    }

    public boolean isReport()
    {
        return isReport;
    }

    public void setIsReport(boolean isReport)
    {
        this.isReport = isReport;
    }

    public enum PHASE
    {
        /**
         * task total运行的时间，前10为框架统计，后面为部分插件的个性统计
         */
        TASK_TOTAL(0),

        READ_TASK_INIT(1),
        READ_TASK_PREPARE(2),
        READ_TASK_DATA(3),
        READ_TASK_POST(4),
        READ_TASK_DESTROY(5),

        WRITE_TASK_INIT(6),
        WRITE_TASK_PREPARE(7),
        WRITE_TASK_DATA(8),
        WRITE_TASK_POST(9),
        WRITE_TASK_DESTROY(10),

        /**
         * SQL_QUERY: sql query阶段, 部分reader的个性统计
         */
        SQL_QUERY(100),
        /**
         * 数据从sql全部读出来
         */
        RESULT_NEXT_ALL(101),

        /**
         * only odps block close
         */
        ODPS_BLOCK_CLOSE(102),

        WAIT_READ_TIME(103),

        WAIT_WRITE_TIME(104),

        TRANSFORMER_TIME(201);

        private final int val;

        PHASE(int val)
        {
            this.val = val;
        }

        public int toInt()
        {
            return val;
        }
    }

    public enum ACTION
    {
        START,
        END
    }
}
