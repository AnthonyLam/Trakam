
class Maestro:

    def __init__(self, uart, limit_min, limit_max, speed, bot, top):
        self.uart = uart
        self.min = limit_min
        self.max = limit_max
        self.speed = speed
        self._bot_state = 1500
        self._top_state = 1500
        self.bot = bot
        self.top = top
        self.send_servo_command(self.bot, self._bot_state)
        self.send_servo_command(self.top, self._top_state)

    def send_servo_command(self, channel: int, target: int):
        target *= 4
        low_bits = 0x7F & target
        high_bits = (target >> 7) & 0x7F
        self.uart.writechar(0x84)
        self.uart.writechar(channel)
        self.uart.writechar(low_bits)
        self.uart.writechar(high_bits)

    def move_left(self, state: int):
        self._bot_state -= ROT_SPEED
        self._bot_state = min(self._bot_state, self.limit_max)
        self.send_servo_command(self.bot, self._bot_state)

    def move_right(self, state: int):
        self._bot_state += ROT_SPEED
        self._bot_state = max(self._bot_state, self.limit_min)
        self.send_servo_command(self.bot, self._bot_state)

    def move_down(self, state: int):
        self._top_state -= ROT_SPEED
        self._top_state = min(self._top_state, self.limit_max)
        self.send_servo_command(self.top, self._top_state)

    def move_up(self):
        self._top_state += ROT_SPEED
        self._top_state = max(self._top_state, self.limit_min)
        self.send_servo_command(self.top, self._top_state)
