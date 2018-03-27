import dotsandboxes.dotsandboxesagent as dba

import sys
import argparse
import logging


logger = logging.getLogger(__name__)
games = {}
agentclass = None


class Agent(dba.DotsAndBoxesAgent):
    def __init__(self, player, nb_rows, nb_cols, timelimit):
        super(Agent, self).__init__(player, nb_rows, nb_cols, timelimit)

    def register_action(self, row, column, orientation, player):
        super(Agent, self).register_action(row, column, orientation, player)
        # adjust agent specific board representation

    def next_action(self):
        # TODO implement
        # return row, column, orientation
        return None


# Adapted from provided code
def main(argv=None):
    global agentclass
    parser = argparse.ArgumentParser(description='Start agent to play Dots and Boxes')
    parser.add_argument('--verbose', '-v', action='count', default=0, help='Verbose output')
    parser.add_argument('--quiet', '-q', action='count', default=0, help='Quiet output')
    parser.add_argument('port', metavar='PORT', type=int, help='Port to use for server')
    args = parser.parse_args(argv)

    logger.setLevel(max(logging.INFO - 10 * (args.verbose - args.quiet), logging.DEBUG))
    logger.addHandler(logging.StreamHandler(sys.stdout))

    agentclass = Agent
    dba.agentclass = Agent
    dba.start_server(args.port)


if __name__ == "__main__":
    sys.exit(main())

