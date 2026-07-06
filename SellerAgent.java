package projet;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SellerAgent extends DefaultAgent implements Constants {

    //initialise avec le prix par défaut DEFAULT_PRICE transformé en chaine de caractères
    private String content = "All our guide are busy during the period of your trek ";
    private ACLMessage message;
    private int performative = ACLMessage.REFUSE;

    /**
     * Methode setup d’initalisation qui récupère l’argument prix (doArguments)
     * sous forme de chaine de caractères et ajoute le comportement
     * SellerBehaviour
     */
    @Override
    public void setup() {
        doArguments();
        addBehaviour(new SellerBehaviour(this));
        trace("Initialisation : agent après traitement de l'argument content " + this);
    }

    @Override
    public String toString() {
        String value = "\n Local Name :: " + getLocalName();
        value += "\n Performative :: " + getPerformative();
        value += "\n Content :: " + getContent();
        return value;
    }

    private ACLMessage getMessage() {
        return this.message;
    }

    private void setMessage(ACLMessage message) {
        this.message = message;
    }

    private String getContent() {
        return content;
    }

    private void doArguments() {
        if (getArguments() != null && getArguments().length > 0) {
            setContent(getArguments()[0].toString());
            setPerformative(ACLMessage.PROPOSE);
        }
    }

    private void setPerformative(int perf) {
        this.performative = perf;
    }

    private void setContent(String price) {
        this.content = price;
    }

    private int getPerformative() {
        return performative;
    }

    /**
     * comportement SellerBehaviour Définr les méthodes onStart, et onEnd() avec
     * terminaison de l’agent
     */
    class SellerBehaviour extends FSMBehaviour {

        /**
         * Etats des agents vendeurs
         */
        private final static String WAITING = "WAITING";
        private final static String PROPOSING = "PROPOSING";
        private final static String REFUSING = "REFUSING";
        private final static String WINNING = "WINNING";
        private final static String LOSING = "LOSING";

        public SellerBehaviour(Agent agent) {
            super(agent);

            registerFirstState(new HandleMessageBehaviour(agent), WAITING);
            registerState(new SendMessageBehaviour(agent), PROPOSING);
            registerLastState(new SendMessageBehaviour(agent), REFUSING);
            registerLastState(new WinningBehaviour(agent), WINNING);
            registerLastState(new LosingBehaviour(agent), LOSING);

            registerTransition(WAITING, PROPOSING, ACLMessage.CFP * ACLMessage.PROPOSE);
            registerTransition(WAITING, REFUSING, ACLMessage.CFP * ACLMessage.REFUSE);
            registerTransition(WAITING, WINNING, ACLMessage.ACCEPT_PROPOSAL * ACLMessage.PROPOSE);
            //registerTransition(WAITING, WINNING, ACLMessage.ACCEPT_PROPOSAL * ACLMessage.REFUSE);
            registerTransition(WAITING, LOSING, ACLMessage.REJECT_PROPOSAL * ACLMessage.PROPOSE);
            //registerTransition(WAITING, LOSING, ACLMessage.REJECT_PROPOSAL * ACLMessage.REFUSE);

            registerDefaultTransition(PROPOSING, WAITING);
        }

        @Override
        public void onStart() {
            super.onStart();
        }

        @Override
        public int onEnd() {
            getAgent().doDelete();
            return super.onEnd();
        }

        /**
         * Comportement simple HandleMessageBehaviour Réception des messages
         * CFP, REJECT_PROPOSAL ou ACCEPT_PROPOSAL de l’agent lola Retourne en
         * fin de comportement le produit de la peformative du message reçu et
         * de la performative native de l'agent définir les méthodes onStart
         * action done et onEnd
         */
        class HandleMessageBehaviour extends SimpleBehaviour {

            private boolean finished = false;
            private int exitValue = 0;

            public HandleMessageBehaviour(Agent a) {
                super(a);
            }

            @Override
            public void onStart() {
                super.onStart();
                finished = false;
            }

            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.CFP),
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                        )
                );

                ACLMessage msg = getAgent().receive(mt);
                if (msg != null) {
                    setMessage(msg);
                    exitValue = msg.getPerformative() * getPerformative();
                    finished = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return finished;
            }

            @Override
            public int onEnd() {
                super.onEnd();
                return exitValue;
            }
        }

        /* fin du comportement HandleMessageBehaviour */
        /**
         * Comportement SendMessageBehaviour qui envoie une réponse au message
         * reçu suivant la performative de l’agent (Refuse ou propose) et le
         * contenu (message par défaut ou le prix Ce comportement factorise le
         * ProposeBehaviour et le RefuseBehaviour
         *
         */
        class SendMessageBehaviour extends OneShotBehaviour {

            ACLMessage reply;

            private void setReply(ACLMessage reply) {
                this.reply = reply;
            }

            private ACLMessage getReply() {
                return this.reply;
            }

            public SendMessageBehaviour(Agent agent) {
                super(agent);
            }

            @Override
            public void onStart() {
                super.onStart();
                ACLMessage msg = getMessage();
                if (msg != null) {
                    reply = msg.createReply();
                    reply.setPerformative(getPerformative());
                    reply.setContent(getContent());
                    setReply(reply);
                    trace("Initialisation de la proposition :: ", this, getAgent());
                    trace("Proposition \n" + getReply().toString());
                }
            }

            @Override
            public void action() {
                if (getReply() != null) {
                    trace("Envoi de la proposition :: ", this, getAgent());
                    getAgent().send(getReply());
                }
            }

            /*
             * methode de terminaison
             */
            @Override
            public int onEnd() {
                trace("Terminaison du comportement ::  ", this, getAgent());
                return super.onEnd();
            }
        }/* fin du comportement SendMessageBehaviour */

        // Définissez les comportements associés aux états WINNING et LOSING 
        class WinningBehaviour extends OneShotBehaviour {

            public WinningBehaviour(Agent agent) {
                super(agent);
            }

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void action() {
                trace("action :: " + getAgent().getLocalName() + " est gagnant avec une proposition de " + getContent() + " euros");
            }

            @Override
            public int onEnd() {
                return super.onEnd();
            }
        }/* fin du comportement WinningBehaviour */

        class LosingBehaviour extends OneShotBehaviour {

            public LosingBehaviour(Agent agent) {
                super(agent);
            }

            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void action() {
                trace("action :: " + getAgent().getLocalName() + " est perdant avec une proposition de " + getContent());
            }

            @Override
            public int onEnd() {
                return super.onEnd();
            }
        }/* fin du comportement LosingBehaviour */

    }
    /* fin de la classe SellerBehaviour */

} // end SellerAgent
