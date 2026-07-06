package projet;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import testjade.DefaultAgent;

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
        System.out.println("Initialisation : " + getLocalName() + " après traitement de l'argument content. État de l'agent : " + this);
        //trace("Initialisation : agent après traitement de l'argument content " + this);
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
        if (getArguments().length > 0) {
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
            registerState(new SendMessageBehaviour(agent), REFUSING);
            registerState(new WinningBehaviour(agent), WINNING);
            registerState(new LosingBehaviour(agent), LOSING);

            registerTransition(WAITING, PROPOSING, ACLMessage.CFP * ACLMessage.PROPOSE);
            registerTransition(WAITING, REFUSING, ACLMessage.CFP * ACLMessage.REFUSE);
            registerTransition(WAITING, WINNING, ACLMessage.ACCEPT_PROPOSAL * ACLMessage.PROPOSE);
            registerTransition(WAITING, WINNING, ACLMessage.ACCEPT_PROPOSAL * ACLMessage.REFUSE);
            registerTransition(WAITING, LOSING, ACLMessage.REJECT_PROPOSAL * ACLMessage.PROPOSE);
            registerTransition(WAITING, LOSING, ACLMessage.REJECT_PROPOSAL * ACLMessage.REFUSE);

            registerDefaultTransition(PROPOSING, WAITING);
            registerDefaultTransition(REFUSING, WAITING);
        }

        // onStart et onEnd 
        @Override
        public void onStart() {
            super.onStart();
            System.out.println("Démarrage du comportement FSM pour " + getAgent().getLocalName());
        }

        @Override
        public int onEnd() {
            System.out.println("Terminaison du comportement FSM pour " + getAgent().getLocalName());
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
        public class HandleMessageBehaviour extends SimpleBehaviour {

            private boolean finished = false;
            private int exitValue = 0;

            public HandleMessageBehaviour(Agent a) {
                super(a);
            }

            @Override
            public void onStart() {
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
                return exitValue;
            }
        }/* fin du comportement HandleMessageBehaviour */

        /**
         * Comportement SendMessageBehaviour qui envoie une réponse au message
         * reçu suivant la performative de l’agent (Refuse ou propose) et le
         * contenu (message par défaut ou le prix Ce comportement factorise le
         * ProposeBehaviour et le RefuseBehaviour
         *
         */

        public class SendMessageBehaviour extends OneShotBehaviour {

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

            //[A compléter – methode d’initialisation qui crée la réponse étant donner le message reçu]
            @Override
            public void onStart() {
                ACLMessage msg = getMessage();
                if (msg != null) {
                    reply = msg.createReply();
                    reply.setPerformative(getPerformative());
                    reply.setContent(getContent());
                    setReply(reply);
                    System.out.println("Initialisation de la réponse pour " + getAgent().getLocalName());
                }
            }

            @Override
            public void action() {
                if (getReply() != null) {
                    System.out.println("Envoi de la réponse par " + getAgent().getLocalName() + " : \n" + getReply());
                    getAgent().send(getReply());
                }
            }

            @Override
            public int onEnd() {
                System.out.println("Terminaison du comportement d'envoi pour " + getAgent().getLocalName());
                return super.onEnd();
            }

        }/* fin du comportement SendMessageBehaviour */

        //Définissez les comportements associés aux états WINNING et LOSING 
        public class WinningBehaviour extends OneShotBehaviour {

            public WinningBehaviour(Agent agent) {
                super(agent);
            }

            @Override
            public void action() {
                System.out.println("action :: " + getAgent().getLocalName() + " est gagnant avec une proposition de " + getContent() + " euros");
            }
        }/* fin du comportement WinningBehaviour */

        class LosingBehaviour extends OneShotBehaviour {

            public LosingBehaviour(Agent agent) {
                super(agent);
            }

            @Override
            public void action() {
                System.out.println("action :: " + getAgent().getLocalName() + " est perdant avec une proposition de " + getContent());
            }
        }/* fin du comportement LosingBehaviour */

    }// end SellerBehaviour

}//end Seller Agent
