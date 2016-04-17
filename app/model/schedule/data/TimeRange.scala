package model.schedule.data

/**
  * @author fabian 
  *         on 04.04.16.
  */
object TimeRange {
  implicit def time2TimeRange(time: Time): TimeRange = {
    TimeRange(time.startHour, time.startMinute, time.stopHour, time.stopMinute)
  }
}

case class TimeRange(startHour: Int, startMinute: Int, stopHour: Int, stopMinute: Int) extends Ordered[TimeRange] {
  override def compare(that: TimeRange): Int = {
    List(this.startHour.compareTo(that.startHour), this.startMinute.compareTo(that.startMinute),
      this.stopHour.compareTo(that.stopHour), this.stopMinute.compareTo(that.stopMinute)).find(_ != 0) match {
      case Some(result) => result
      case None => 0
    }
  }

  override def toString = {
    def format(x:Int) ={
      "%02d".format(x)
    }

    new StringBuilder().append(format(startHour)).append(":")
      .append(format(startMinute)).append(" - ")
      .append(format(stopHour)).append(":")
      .append(format(stopMinute)).toString()
  }
}